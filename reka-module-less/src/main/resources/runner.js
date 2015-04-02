
function lessToCss(less) {
	var parser = new(less.Parser);
	parser.parse(lessSrc.main, function(err, tree) {
	    if (err) return print(err);
	    out.css = tree.toCSS({ compress: true });
	});
}