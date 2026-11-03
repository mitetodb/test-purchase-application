(function(){var c=navigator.connection||{};
var low=matchMedia('(prefers-reduced-motion: reduce)').matches||c.saveData||/(^|-)(2g|3g)$/.test(c.effectiveType||'');
var narrow=matchMedia('(max-width:768px)').matches;
document.querySelectorAll('[data-bgvideo]').forEach(function(w){var v=w.querySelector('video');if(!v)return;
var d=w.dataset,portrait=narrow&&d.portraitMp4;
if(low||(narrow&&!portrait)){w.classList.add('is-still');v.removeAttribute('autoplay');v.preload='none';v.pause();Array.from(v.querySelectorAll('source')).forEach(function(s){s.remove()});v.load();return}
if(portrait){/* approved exception: phones play the dedicated 9:16 login loop */
w.classList.add('is-portrait');v.poster=d.portraitPoster||v.poster;
var s=v.querySelectorAll('source');if(s[0])s[0].src=d.portraitWebm;if(s[1])s[1].src=d.portraitMp4;v.load()}
v.addEventListener('error',function(){w.classList.add('is-still')},true);
var vis=true;function upd(){if(vis&&!document.hidden){var p=v.play();p&&p.catch(function(){})}else v.pause()}
if('IntersectionObserver'in window)new IntersectionObserver(function(e){vis=e[0].isIntersecting;upd()}).observe(w);
document.addEventListener('visibilitychange',upd);upd()})})();
