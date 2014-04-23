### these are running applications
          
<i class="fa fa-long-arrow-left"></i> they are alive!

they are each made up of:

- zero or more _dependencies_
- one or more _flows_
- one or more _triggers_ which trigger the flows

#### dependencies

these are similar _modules_ /  _libraries_ / _services_.

they may be inert like a _library_ (e.g. `jade` template rendering)

or active like a _service_ (e.g. `h2` database)

#### flows

these are the **meat** of the application.

they represent a flow of execution, you can see that in the visualizations.

the flows do nothing until they are triggered by something.

#### triggers

these initiate _flows_ to run

they might be internal (e.g. a timer, using `every`)

or start their own infrastructure for listening to the external world (e.g. `http`)