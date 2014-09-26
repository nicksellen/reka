var classSet = React.addons.classSet;

function setApps(apps) {
  if (!this.isMounted()) return;
  this.setState({ 
    apps: apps.map(item => {
      var app = item.app;
      app.id = item.id;
      return app;
    }) 
  });
}

var RekaUI = React.createClass({
  getInitialState: function() {
    return {
      apps: [],
      appid: null,
      page: null,
      title: null,
      sidebar: true,
      disconnected: false
    };
  },
  getDefaultProps: function(){
    return {
      renderPage: function(){}.bind(this)
    };
  },
  findApp: function(appid) {
    for (var i = 0; i < this.state.apps.length; i++) {
      var app = this.state.apps[i];
      if (app.id === appid) return app;
    }
    return null;
  },
  findFlow: function(app, flowid) {
    for (var i = 0; i < app.flows.length; i++) {
      var flow = app.flows[i];
      if (flow.name === flowid) return flow;
    }
    return null;
  },
  componentDidMount: function(){

    page('/', function(req){
      this.setProps({
        renderPage: function(){}.bind(this)
      });
      this.setState({ appid: null, sidebar: true });
    }.bind(this));

    page(new RegExp("\/apps\/(.*)\/flows\/(.*)"), function(req){
      var appid = req.params[0];
      var flowid = req.params[1];
      if (!this.findApp(appid)) return page('/');
      this.setProps({
        renderPage: function(){
          var app = this.findApp(appid);
          var flow = this.findFlow(app, flowid);
          if (app && flow) {
            return <Flow app={app} flow={flow}/>;
          } else {
            setTimeout(function(){
              page('/');
            }, 0);
          }
        }.bind(this)
      });
      this.setState({ appid: appid, sidebar: false });
    }.bind(this));

    page(new RegExp("\/apps\/(.*)"), function(req){
      var appid = req.params[0];
      if (!this.findApp(appid)) return page('/');
      this.setProps({
        renderPage: function(){
          var app = this.findApp(appid);
          if (app) {
            return <AppDetails app={app}/>;
          } else {
            setTimeout(function(){
              page('/');
            }, 0);
          }
        }.bind(this),
        renderDebug: function(){
          var app = this.findApp(appid);
          return <div className="app-status">
          <pre>{JSON.stringify(app.status, undefined, 2)}</pre>
        </div>;
        }.bind(this)
      });
      this.setState({ appid: appid, sidebar: true });
    }.bind(this));

    // initial load
    $.get('/api/apps', function(apps){
      setApps.call(this, apps);
      page.start();
    }.bind(this));

    // subsequent updates
    Reka.ws.recv(function(data){
      if (data.apps) setApps.call(this, data.apps);
    }.bind(this));

    Reka.ws.disconnect(function(data){
      this.setState({ disconnected: true });
    }.bind(this));

  },
  render: function(){
    return <div id="panel">
      {this.state.sidebar && <div id="sidebar">
        <h1>
          <a href="/">
            <span className="icon icon-radio-checked"></span>reka admin
          </a>
        </h1>
        <AppList selected={this.state.appid} apps={this.state.apps} />
      </div>}
      <section id="content">
        {this.props.renderPage()}
      </section>
      <section id="content-offside">
        <div className="inner">
          {this.props.renderDebug && this.props.renderDebug()}
        </div>
      </section>
      {this.state.disconnected && <Disconnected/>}
    </div>;
  }
});

React.renderComponent(<RekaUI/>, document.getElementById('app'));