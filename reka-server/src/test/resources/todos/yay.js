
var me = {
	not_done: [],
	done: []
};

for (var i = 0; i < todos.results.length; i++) {
	var todo = todos.results[i];
	delete todo.user;
	if (todo.done) {
		me.done.push(todo.name);
	} else {
		me.not_done.push(todo.name);
	}
}

me;