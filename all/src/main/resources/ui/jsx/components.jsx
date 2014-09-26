
var Flow = React.createClass({
  render: function(){
    var app = this.props.app;
    var flow = this.props.flow;
    var src = "/api/apps/" + app.id + "/flows/" + flow.name + ".svg?v" + app.version;
    var appPath = '/apps/' + app.id;
    return <div className="bigflow">
      <div className="header">
        <h1>
          <span className="app-name">{app.name}</span>
          <span className="flow-name">
            {flow.name.split('/').map(function(part){
              return <span key={part} className="part">{part}</span>;
            })}
          </span>
        </h1>
        <a href={appPath}>back to app</a>
      </div>
      <div className="content">
        <img src={src}/>     
      </div>
    </div>;
  }
});
// <img src={src}/>
// <object data={src} type="image/svg+xml"/>

var Disconnected = React.createClass({
  render: function(){
    return <div className="disconnected"><h1>connection lost</h1></div>;
  }
});

var AppDetails = React.createClass({

  destroy: function(){
    $.ajax({
      url: '/api/apps/' + this.props.app.id,
      type: 'DELETE',
      success: function(result) { /* nothing! update will come by ws */ }
    });
  },
  showFlow: function(flow) {
    page('/apps/' + this.props.app.id + '/flows/' + flow.name);
  },
  render: function(){
    var app = this.props.app;
    return (
      <div className="app-details">
        <h1 className="app-name">{app.name}</h1>
        <ul className="app-network">
          {app.network.map(function(item){
            if (item.url) {
              return <li key={item.url}>
                <a target="_blank" href={item.url}>{item.url}</a>
              </li>;
            }
          })}
        </ul>
        <ul className="app-actions">
          <li>
            <button className="destroy" onClick={this.destroy}>
              <span className="icon icon-switch"></span>
              undeploy
            </button>
          </li>
        </ul>
        <ul className="flowlist">
          {app.flows.map(function(flow){
            var src = "/api/apps/" + app.id + "/flows/" + flow.name + ".svg?v" +app.version;
            return <li key={src} onClick={this.showFlow.bind(this, flow)}>
              <span className="flow-name">
                {flow.name.split('/').map(function(part){
                  return <span key={part} className="part">{part}</span>;
                })}
              </span>
              <div className="tinyflow">
                <img src={src} />
              </div>
            </li>;
          }.bind(this))}
        </ul>
      </div>
    );
  }
});

var AppList = React.createClass({
  filterChanged: function(e) {
    this.setState({ query: e.target.value });
  },
  filter: function(app) {
    var q = this.state && this.state.query;
    if (!q) return true;
    return app.name.indexOf(q) !== -1 || app.id.indexOf(q) !== -1;
  },
  render: function() {
    return (
      <div className="app-list-container">
        <input className="app-filter" type="text" placeholder="filter apps" onChange={this.filterChanged}/>
        <ul className="app-list">
          {this.props.apps.filter(this.filter).map(function(app){
            var url = '/apps/' + app.id;
            var classes = classSet({
              title: true,
              active: this.props.selected === app.id
            });
            return <li key={app.id}>
              <a className={classes} href={url}>
                <span className="app-name">{app.name}</span>
                <span className="app-id">{app.id}</span>
              </a>
            </li>;
          }.bind(this))}
        </ul>
      </div>
    );
  }
});