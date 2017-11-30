function setEditButonNm() {
 $(document).ready(function () {

 var style = document.createElement("link");
 style.href="http://overturetool.org/css/releases.css";
 style.rel="stylesheet";
 	document.head.appendChild(style);

        var node = document.getElementById("edit_page_div");
       	var n =location.pathname.split("/").slice(-1)+"";
		var link ="https://github.com/overturetool/overturetool.github.io/edit/master/_netmeetings/"+n.substring(0,n.lastIndexOf("."))+".md";
    node.innerHTML  = "<a class=\"button primary\" href=\""+ link+"\">Edit</a>" ;
    });
	
	
 };