
(function() {

    // http://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/
    // https://github.com/sterlingwes/RandomColor/blob/master/rcolor.js

    var RColor = function() {
      this.hue      = Math.random(),
      this.goldenRatio  = 0.618033988749895;
      this.hexwidth   = 2;
    };

    RColor.prototype.hsvToRgb = function (h,s,v) {
      var h_i = Math.floor(h*6),
        f   = h*6 - h_i,
        p = v * (1-s),
        q = v * (1-f*s),
        t = v * (1-(1-f)*s),
        r = 255,
        g = 255,
        b = 255;
      switch(h_i) {
        case 0: r = v, g = t, b = p;  break;
        case 1: r = q, g = v, b = p;  break;
        case 2: r = p, g = v, b = t;  break;
        case 3: r = p, g = q, b = v;  break;
        case 4: r = t, g = p, b = v;  break;
        case 5: r = v, g = p, b = q;  break;
      }
      return [Math.floor(r*256),Math.floor(g*256),Math.floor(b*256)];
    };

    RColor.prototype.padHex = function(str) {
      if(str.length > this.hexwidth) return str;
      return new Array(this.hexwidth - str.length + 1).join('0') + str;
    };

    RColor.prototype.get = function(hex,saturation,value) {
      this.hue += this.goldenRatio;
      this.hue %= 1;
      if(typeof saturation !== "number")  saturation = 0.5;
      if(typeof value !== "number")   value = 0.95;
      var rgb = this.hsvToRgb(this.hue,saturation,value);
      if(hex)
        return "#" +  this.padHex(rgb[0].toString(16))
              + this.padHex(rgb[1].toString(16))
              + this.padHex(rgb[2].toString(16));
      else 
        return rgb;
    };    


    var NAME_LENGTH = 8;

    function nameToShortName(name) {
      if (!name) return;
      if (name.length > NAME_LENGTH) {
        name = name.slice(0, NAME_LENGTH);
      } else if (name.length < NAME_LENGTH) {
        var i = 0, diff = NAME_LENGTH - name.length;
        for (; i < diff; i++) {
          name = '&nbsp;' + name;
        }
      }
      return name;
    } 

    function Me() {

      this.color = localStorage.getItem('color');

      if (!this.color) {
        this.color = randomColor.get(true, 0.3, 0.99);
        localStorage.setItem('color', this.color);
      }

      this.applyColor();

      this.connectionId;
    }

    Me.prototype.applyColor = function() {
      document.getElementById('compose').style.borderLeftColor = this.color;
    };

    Me.prototype.recolor = function() {
      this.color = randomColor.get(true, 0.3, 0.99);
      localStorage.setItem('color', this.color);
      this.applyColor();
    };

    Me.prototype.is = function(id) {
      return this.connectionId === id;
    }

    function updateName(newName) {
      name = newName;
      localStorage.setItem('name', name);
      currentUser.innerHTML = nameToShortName(name);
    }

    var randomColor = new RColor();

    var me = new Me();
    var users = { };

    users['server'] = {
      color: '#444'
    };
      
    var messageInput = document.getElementById('message');
    var messageList = document.getElementById('message-list');

    var currentUser = document.getElementById('current-user');

    var name = localStorage.getItem('name');

    if (name) updateName(name);

    var Sock = function() {
  
      var socket;
      
      if (!window.WebSocket) {
          window.WebSocket = window.MozWebSocket;
      }

      if (window.WebSocket) {
          socket = new WebSocket('ws://' + location.host);
          socket.onopen = onopen;
          socket.onmessage = onmessage;
          socket.onclose = onclose;
      } else {
          alert("Your browser does not support Web Socket.");
      }

      function onopen(event) {
        console.log('connected!');
      }

      function onmessage(event) {
          var payload = JSON.parse(event.data);
          console.log('received message', payload);
          if (payload['connection-id']) {
            me.connectionId = payload['connection-id'];
            if (!name) updateName(me.connectionId);
          }
          if (payload.data) add(payload.id, payload.data);
      }
      
      function onclose(event) {
        console.log('connection lost');
      }

      function add(id, data) {

        var user;

        if (me.is(id)) {
          user = me;
        } else {
          if (!users[id]) {
            users[id] = {
              color: randomColor.get(true, 0.3, 0.99)
            };
          }
          user = users[id];
        }

        if (!data.name) data.name = id;

        var shortName = nameToShortName(data.name);

        var li = document.createElement('li');

        if (me.is(id)) li.className = 'me';

        li.style.borderLeftColor = user.color;

        var eUser = document.createElement('span');
        eUser.className = 'user';
        eUser.innerHTML = shortName;
        li.appendChild(eUser);

        var eContent = document.createElement('input');
        eContent.className = 'content';
        eContent.value = data.msg;
        li.appendChild(eContent);

        messageList.appendChild(li);
        messageList.scrollTop = messageList.scrollHeight;
      }

      function command(cmd, rest) {
        switch (cmd) {
          case 'name':
            updateName(rest);
            break;
          case 'recolor':
          case 'recolour':
            me.recolor();
            break;
          default:
            console.warn('unknown command [' + cmd + '] with [' + rest + ']');
        }
      }

      function send(event) {
          event.preventDefault();

          var content = event.target.message.value;

          var cmd = /^\/(\S+)(?:\ (.*))?$/.exec(content);

          if (cmd) {
            command(cmd[1], cmd[2]);
          } else {
            if (window.WebSocket) {
                if (socket.readyState == WebSocket.OPEN) {
                    var data = { msg: event.target.message.value };
                    if (name) data.name = name;
                    socket.send(JSON.stringify(data));
                } else {
                    alert("The socket is not open.");
                }
            }
          }
          messageInput.value = '';
          messageInput.focus();
      }
      document.forms.inputform.addEventListener('submit', send, false);
  }
  window.addEventListener('load', function() { new Sock(); }, false);
})(); 