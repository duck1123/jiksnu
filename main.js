window.jQuery = require('jquery');
window.$ = window.jQuery;

var requires = {
  angular: require('angular'),
  angularClipboard: require('angular-clipboard'),
  angularFileUpload: require('angular-file-upload'),
  angularGeolocation: require('angularjs-geolocation'),
  angularHighlightjs: require('angular-highlightjs'),
  angularHotkeys: require('angular-hotkeys'),
  angularMarkdownDirective: require('angular-markdown-directive'),
  angularMaterial: require('angular-material'),
  angularMaterialIcons: require('angular-material-icons'),
  angularMoment: require('angular-moment'),
  angularSanitize: require('angular-sanitize'),
  angularUiRouter: require('angular-ui-router'),
  angularWebsocket: require('angular-websocket'),
  highlight: require('highlight.js'),
  lfNgMdFileInput: require('lf-ng-md-file-input'),
  jquery: require('jquery'),
  jsDataAngular: require('js-data-angular'),
  raven: require('raven-js'),
  ravenAngular: require('raven-js/dist/plugins/angular.js'),
  showdown: require('showdown'),
  uiSelect: require('ui-select'),
  underscore: require('underscore')
};

window.Showdown = requires.showdown;
window.Raven = requires.raven;
window.Raven.Plugins = {};
window.Raven.Plugins.Angular = requires.ravenAngular;
window.requires = requires;

module.exports = requires;
