var forwardConns = {};
var backwardConns = {};

var nodes = {};

for (var i = 0; i < graph.nodes.length; i++) {
  var node = graph.nodes[i];
  nodes[node.id] = node;
}

function addConn(conns, a, b) {
  if (!conns.hasOwnProperty(a)) conns[a] = [];
  conns[a].push(b);
}

for (var i = 0; i < graph.connections.length; i++) {
  var c = graph.connections[i];
  addConn(forwardConns, c.from, c.to);
  addConn(backwardConns, c.to, c.from);
}

function connectedNodesFor(conns, a) {
  return conns.hasOwnProperty(a) ? conns[a] : [];
}

var start, end;
for (var i = 0; i < graph.nodes.length; i++) {
  var n = graph.nodes[i];
  if (n.type === 'START') {
    start = n;
  } else if (n.type === 'END') {
    end = n;
  }
}

function addDistanceFrom(prop, id, d) {
  var otherIds = connectedNodesFor(prop === 's' ? forwardConns : backwardConns, id);
  for (var i = 0; i < otherIds.length; i++) {
    var other = nodes[otherIds[i]];

    if (other.hasOwnProperty(prop)) {
      var prev = other[prop];
      if (d < prev) {
        // found a shorter path
        other[prop] = d;

        if (other.type !== 'END' && other.type !== 'START') {
          addDistanceFrom(prop, other.id, d + 1);  
        }
      }
    } else {
      other[prop] = d;
      if (other.type !== 'END' && other.type !== 'START') {
        addDistanceFrom(prop, other.id, d + 1);  
      }
    }
  }
}

if (start) addDistanceFrom('s', start.id, 0);
if (end) addDistanceFrom('e', end.id, 0); 

function traverseTo(prop, id, callback) {
  var node = nodes[id];
  if (!node.hasOwnProperty(prop)) return;
  var otherIds = connectedNodesFor(prop === 's' ? backwardConns : forwardConns, id);
  for (var i = 0; i < otherIds.length; i++) {
    var other = nodes[otherIds[i]];
    callback(node, other);
    traverseTo(prop, other.id, callback);
  }
}

function highlightPathTo(prop, id) {
  traverseTo(prop, id, function(a, b){
    var attr = prop === 's' ? 'data-highlight-start' : 'data-highlight-end';
    $('#connection__' +  a.id + '__' + b.id).attr(attr, true);
    $('#connection__' +  b.id + '__' + a.id).attr(attr, true);
    $('#' + b.domid).attr(attr, true);
  });
}

$(function() {

  function parseId(val) {
    var vals = val.split('__');
    return parseInt(vals[1], 10);
  }

  $('g.node[id]').each(function(i, e){
    var domid = $(e).attr('id');
    if (!domid) return;
    var node = nodes[parseId(domid)];
    node.domid = domid;
  });

  $('g.node').click(function(e) {
    $('*[data-highlight-start]').removeAttr('data-highlight-start');
    $('*[data-highlight-end]').removeAttr('data-highlight-end');
    $('g.node[data-active]').removeAttr('data-active');
    var domid = $(e.target).parents('g.node').first().attr('id');
    var e = $('#' + domid);
    e.attr('data-active', true);

    var id = parseId(domid);

    highlightPathTo('s', id);
    highlightPathTo('e', id);

  });
});

