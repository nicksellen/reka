(function() {

  var recvs = [], disconnects = [];

  if (!window.Reka) window.Reka = {};

  Reka.ws = {
    recv: function(cb) {
      recvs.push(cb);
    },
    disconnect: function(cb) {
      disconnects.push(cb);
    },
    send: function() {
      console.log('not connected');
    }
  };

  function tell(list, payload) {
    for (var i = 0; i < list.length; i++) {
      list[i](payload);
    }
  }

  function init() {

    var Reka = window.Reka;
  
    var socket;
    
    if (!window.WebSocket) {
        window.WebSocket = window.MozWebSocket;
    }

    if (window.WebSocket) {
        var protocol = location.protocol.replace('http', 'ws');
        var url = protocol + '//' + location.host;
        socket = new WebSocket(url);
        socket.onopen = onopen;
        socket.onmessage = onmessage;
        socket.onclose = onclose;
    } else {
        alert("Your browser does not support Web Socket.");
    }

    function onopen(event) { }

    function onmessage(event) {
      var payload = JSON.parse(event.data);
      tell(recvs, payload);
    }
    
    function onclose(event) {
      tell(disconnects, {});
    }

    Reka.ws.send = function(payload) {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify(payload));
      } else {
        alert("The socket is not open.");
      }
    };

  }

  window.addEventListener('load', init, false);

})(); 
