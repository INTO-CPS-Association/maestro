

function formatSizeUnits(bytes) {
    if ((bytes >> 30) & 0x3FF)
        bytes = (bytes >>> 30) + '.' + (bytes & (3 * 0x3FF)) + 'GB';
    else if ((bytes >> 20) & 0x3FF)
        bytes = (bytes >>> 20) + '.' + (bytes & (2 * 0x3FF)) + 'MB';
    else if ((bytes >> 10) & 0x3FF)
        bytes = (bytes >>> 10) + '.' + (bytes & (0x3FF)) + 'KB';
    else if ((bytes >> 1) & 0x3FF)
        bytes = (bytes >>> 1) + 'Bytes';
    else
        bytes = bytes + 'Byte';
    return bytes;
}


function updateFrontPage() {

    $(document).ready(function () {


        var body = document.getElementById("current-release");
        //tbl  = document.createElement('table');

        //var cell = 0;
        //var tr = tbl.insertRow();
        //var row=0;
        var releaseIndex = 0;

        $.getJSON("https://api.github.com/repos/overturetool/overture/releases", function (result) {
            $.each(result/*.reverse()*/, function (i, field) {

                if (("" + field.tag_name).indexOf("Release") > -1 && field.draft == false && field.prerelease == false) {



                    if (i > 0) {
                        return;
                    }

                    var divVersion = document.getElementById("current-release-version");
                    divVersion.innerHTML = field.tag_name.replace("Release/", "");

                    var divDate = document.getElementById("current-release-data");
                    var publishedAt = new Date(field.published_at);
                    divDate.innerHTML = "(" + moment(field.published_at).format('MMM YYYY') + ")";

                    //var td = tr.insertCell(cell-1);

                    /*  var gitDiv = document.createElement("div");
                      //	gitDiv.className = "githubcontainer";
  
                      var link = document.createElement("a");
                      link.href = field.html_url;
                      link.appendChild(gitDiv);
  
                      var rank = document.createElement("h3");
                      rank.innerHTML = field.name;
                     
                      gitDiv.appendChild(rank);
  
                     
                      //showAssetList();
  
                      body.appendChild(link);*/

                    return;
                }
            });

            //body.appendChild(tbl);
        });
    });

}

function updateDownloadPage() {
    $(document).ready(function () {


        // var body = document.getElementById("current-release");
        //tbl  = document.createElement('table');

        //var cell = 0;
        //var tr = tbl.insertRow();
        //var row=0;
        var releaseIndex = 0;

        $.getJSON("https://api.github.com/repos/overturetool/overture/releases", function (result) {
            $.each(result/*.reverse()*/, function (i, field) {

                if (("" + field.tag_name).indexOf("Release") > -1 && field.draft == false && field.prerelease == false) {

                    var releaseVersion = field.tag_name.replace("Release/", "");
                    var releaseDate = moment(field.published_at).format('MMM YYYY');
                    var releaseName = field.name;
                    var releaseUrl = field.html_url;
                    var assets = field.assets;


                    if (i == 0) {
                        //latests release
                        var releaseTitle = document.createElement("h3");
                        releaseTitle.innerHTML = releaseName + " (" + countTotalAssetDownloads(assets) + " downloads)";

                        var releaseLink = document.createElement("a");
                        releaseLink.href = releaseUrl;

                        releaseLink.appendChild(releaseTitle);

                        var currentReleaseDiv = document.getElementById("div-current-release");
                        currentReleaseDiv.appendChild(releaseLink);
                        currentReleaseDiv.appendChild(buildAssetList(releaseUrl, assets));
                    } else {

                        if (i == 1) {
                            var tblBody = document.createElement("tbody");
                            tblBody.id = "release-history-table-body";

                            var tbl = document.createElement('table');
                            tbl.appendChild(tblBody);
                            var releaseHistoryDiv = document.getElementById("div-release-history");
                            releaseHistoryDiv.appendChild(tbl);
                        }
                        var tblBody = document.getElementById("release-history-table-body");
                        var row = document.createElement("tr");
                        {
                            var cell = document.createElement("td");
                            var cellText = document.createTextNode(releaseName + " (" + countTotalAssetDownloads(assets) + " downloads)");

                            cell.appendChild(cellText);
                            row.appendChild(cell);
                            tblBody.appendChild(row);
                        }
                        {
                            var cell = document.createElement("td");
                            var cellText = document.createTextNode(releaseDate);

                            cell.appendChild(cellText);
                            row.appendChild(cell);
                            tblBody.appendChild(row);
                        }
                        {
                            var cell = document.createElement("td");
                            var cellText = document.createTextNode("release note");

                            var releaseLink = document.createElement("a");
                            releaseLink.href = releaseUrl;

                            releaseLink.appendChild(cellText);

                            cell.appendChild(releaseLink);
                            row.appendChild(cell);
                            tblBody.appendChild(row);
                        }

                        return;
                    }
                }
            });

            //body.appendChild(tbl);
        });
    });
}

function countTotalAssetDownloads(assets) {

    var count = 0;
    $.each(assets, function (i, asset) {
        if (("" + asset.name).indexOf("Overture") > -1) {
            count = count + asset.download_count;
        }
    });
    return count;
}

function buildAssetList(baseUrl, assets) {
    var ul = document.createElement("ul");
    ul.className = "release-downloads";

    $.each(assets/*.reverse()*/, function (i, asset) {

        var li = document.createElement("li");
        var da = document.createElement("a");
        da.href = baseUrl.replace("tag", "download") + "/" + asset.name;
        da.className = "button primary";

        var dsl = document.createElement("span");
        dsl.className = "octicon octicon-arrow-down";


        var dlss = document.createElement("span");
        dlss.className = "overture-tooltipped";//"tooltipped tooltipped-s";
        //  dlss.aria-label=formatSizeUnits(ass.size);
        dlss.innerHTML = asset.name;

        li.appendChild(da);
        da.appendChild(dsl);
        da.appendChild(dlss);

        ul.appendChild(li);

    });
    //gitDiv.appendChild(ul);
    return ul;
}
