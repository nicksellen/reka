define(
		'ace/mode/flow',
		function(require, exports, module) {

			var oop = require("ace/lib/oop");
			var TextMode = require("ace/mode/text").Mode;
			var Tokenizer = require("ace/tokenizer").Tokenizer;
			var FlowHighlightRules = require("ace/mode/flow_highlight_rules").FlowHighlightRules;

			var Mode = function() {
				this.$tokenizer = new Tokenizer(new FlowHighlightRules()
						.getRules());
			};
			oop.inherits(Mode, TextMode);

			exports.Mode = Mode;
		});

define(
		'ace/mode/flow_highlight_rules',
		function(require, exports, module) {

			var oop = require("../lib/oop");
			var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;

			var FlowHighlightRules = function() {

				this.$rules = {
					start : [ {
						token : "comment",
						regex : "#.*$"
					}, {
						token : "paren.rparen",
						regex : /\}/
					}, {
						token : 'support.function.config-kw',
						regex : /\S+$/,
					}, {
						token : "support.function.config-k",
						regex : /\S+/,
						next : "key"
					} ],
					key : [ {
						token : "punctuation.space",
						regex : /\ $/,
						next : 'start'
					}, {
						token : "punctuation.space",
						regex : /\s+/,
						next : 'val'
					} ],
					val : [
							{
								token : [ 'entity.name.tag.doc_start',
										'entity.name.tag.doc_type' ],
								regex : /\ ?(<<\-)(.*)$/,
								next : 'doc'
							}, {
								token : 'paren.lparen',
								regex : '\ ?{$',
								next : 'start'
							}, {
								token : 'val',
								regex : /$/,
								next : 'start',
							}, {
								token : 'val',
								regex : /./
							} ],
					doc : [ {
						token : "entity.name.tag.doc_end",
						regex : /^\s*\-\-\-\s*$/,
						next : 'start'
					}, {
						token : 'string.doc',
						regex : /^.*$/
					} ]
				};
			};

			oop.inherits(FlowHighlightRules, TextHighlightRules);

			exports.FlowHighlightRules = FlowHighlightRules;
		});
