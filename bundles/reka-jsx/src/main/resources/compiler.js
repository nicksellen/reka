var src = "/** @jsx React.DOM */\n\n" + data.src; 
data.code = module.exports.transform(src, { harmony: true }).code;