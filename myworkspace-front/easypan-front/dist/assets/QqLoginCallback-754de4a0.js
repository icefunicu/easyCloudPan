import{a as o,u as n,b as s,f as c,h as u}from"./index-e609a1ae.js";const f={__name:"QqLoginCallback",setup(i){const{proxy:t}=o(),r=n();s();const l={logincallback:"/qqlogin/callback"};return(async()=>{let e=await t.Request({url:l.logincallback,params:r.currentRoute.value.query,errorCallback:()=>{r.push("/")}});if(!e)return;let a=e.data.errorCallback||"/";a=="/login"&&(a="/"),t.VueCookies.set("userInfo",e.data.userInfo,0),r.push(a)})(),(e,a)=>(c(),u("div",null,"登录中,请勿刷新页面"))}};export{f as default};
