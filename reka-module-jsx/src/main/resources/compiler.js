var src = "/** @jsx React.DOM */\n\n" + data.src; 
return module.exports.transform(src, { harmony: true }).code;