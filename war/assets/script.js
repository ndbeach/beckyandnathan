
/* Force the www subdomain */
if (window.location.host == "beckyandnathan.org") {
	window.location.replace(window.location.protocol
			+ "//www.beckyandnathan.org"
			+ (window.location.pathname.charAt(0) != "/" ? "/" : "")
			+ window.location.pathname);
}

/* Google Analytics */
if (window.location.host == "www.beckyandnathan.org") {
	(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
	(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
	m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
	})(window,document,'script','//www.google-analytics.com/analytics.js','ga');

	ga('create', 'UA-52498343-1', 'auto');
	ga('send', 'pageview');
}