"use strict";(self.webpackChunk_internetarchive_bookreader=self.webpackChunk_internetarchive_bookreader||[]).push([[951],{5842:function(e,o,t){var n={};function r(){var e=arguments.length>0&&void 0!==arguments[0]?arguments[0]:document;try{return e.cookie,!1}catch(e){return!0}}t.r(n),t.d(n,{areCookiesBlocked:function(){return r},getItem:function(){return u},removeItem:function(){return i},setItem:function(){return c}}),t(2320),t(4043),t(2462),t(7267),t(2003),t(2826);var a=r();function u(e){return a||!e?null:decodeURIComponent(document.cookie.replace(new RegExp("(?:(?:^|.*;)\\s*"+encodeURIComponent(e).replace(/[\-\.\+\*]/g,"\\$&")+"\\s*\\=\\s*([^;]*).*$)|^.*$"),"$1"))||null}function c(e,o,t,n,r,u){return!a&&(document.cookie=encodeURIComponent(e)+"="+encodeURIComponent(o)+(t?"; expires=".concat(t.toUTCString()):"")+(r?"; domain=".concat(r):"")+(n?"; path=".concat(n):"")+(u?"; secure":""),!0)}function i(e,o,t){return!a&&!!hasItem(e)&&(document.cookie=encodeURIComponent(e)+"=; expires=Thu, 01 Jan 1970 00:00:00 GMT"+(t?"; domain=".concat(t):"")+(o?"; path=".concat(o):""),!0)}var s,d=t(5311);BookReader.docCookies=n,d.extend(BookReader.defaultOptions,{enablePageResume:!0,resumeCookiePath:null}),BookReader.prototype.init=(s=BookReader.prototype.init,function(){var e=this;s.call(this),this.options.enablePageResume&&this.bind(BookReader.eventNames.fragmentChange,(function(){var o=e.paramsFromCurrent();e.updateResumeValue(o.index)}))}),BookReader.prototype.getResumeValue=function(){var e=BookReader.docCookies.getItem("br-resume");return null!==e?parseInt(e):null},BookReader.prototype.getCookiePath=function(e){return e.match(".+?(?=/page/|/mode/|$)")[0]},BookReader.prototype.updateResumeValue=function(e,o){var t=new Date(+new Date+12096e5),n=this.options.resumeCookiePath||this.getCookiePath(window.location.pathname);BookReader.docCookies.setItem(o||"br-resume",e,t,n,null,!1)}}},function(e){e(e.s=5842)}]);
//# sourceMappingURL=plugin.resume.js.map