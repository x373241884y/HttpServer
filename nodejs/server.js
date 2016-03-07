#!/usr/bin/env node
var config = {
    port : 8080,
    root : process.cwd(),
};
var net = require('net'),
path = require('path'),
fs = require('fs'),
request = {},
response = {},
MIME = {
    text : 'text/plain',
    html : 'text/html',
    css : 'text/css',
    js : 'application/javascript',
    json : 'application/json'
};

function formatDate(date, style) { //date format util
    var y = date.getFullYear();
    var M = "0" + (date.getMonth() + 1);
    M = M.substring(M.length - 2);
    var d = "0" + date.getDate();
    d = d.substring(d.length - 2);
    var h = "0" + date.getHours();
    h = h.substring(h.length - 2);
    var m = "0" + date.getMinutes();
    m = m.substring(m.length - 2);
    var s = "0" + date.getSeconds();
    s = s.substring(s.length - 2);
    return style.replace('yyyy', y).replace('MM', M).replace('dd', d).replace('HH', h).replace('mm', m).replace('ss', s);
}

function parse_headers(content) {
    var array = content.match(/^(.*)\s(\/.*)\s(HTTP\/\d\.\d)/);
    request.method = array[1];
    request.uri = array[2];
    request.protocol = array[3];
    var lines = content.split(/\n/);
    for (var i = 0; i < lines.length; i++) {
        var sublines = lines[i].match(/^([^()<>\@,;:\\"\/\[\]?={} \t]+):\s*(.*)/i);
        sublines && (request[sublines[1]] = sublines[2]);
    }
}

function init_response(request) {
    var uri = request.uri;
    if (/\?.*/.test(uri)) {
        uri = uri.replace(/\?.*/, "");
    }
    // if (/\/$/.test(uri)) {
    //     uri += "index.html";
    // }
    if (/\w+\.html$/.test(uri)) {
        response.mime = MIME.html;
    } else if (/\w+\.css$/.test(uri)) {
        response.mime = MIME.css;
    } else if (/\w+\.js$/.test(uri)) {
        response.mime = MIME.js;
    } else if (/\w+\.json/.test(uri)) {
        response.mime = MIME.json;
    } else if (/\w+\.do/.test(uri)) {
        if (request.$Referer) {
            var subffix = uri.replace(/(\w+)\.do/, "$1.json");
            var prefix = request.$Referer.replace(/htmls\/(.*\/)\w+\.html$/, "/data/$1");
            var uri = prefix + subffix;
            response.mime = MIME.json;
        } else {
            return 1;
        }
    } else {
        response.mime = MIME.html;
    }
    response.resource = path.join(config.root, uri);
}

function resp_filelist(socket, resource) {
    socket.write("HTTP/1.0 200 OK\n");
    socket.write("Content-Type: text/html" + ";charset: UTF-8\n");
    socket.write("Date: " + new Date() + "\n");
    socket.write("Server: xyserver\n");
    socket.write("\n");
    if(/[^\/]$/.test(config.root)){
        config.root=path.join(config.root,"/");
    }
    var current = resource.substr(config.root.length - 1);
    socket.write("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8' /> <title>Index of ./</title></head><body><h1>Directory:" + current + "</h1><table border='0'><tbody>");
    socket.write("<tr><td><a href='../'>Parent Directory</a></td><td></td><td></td></tr>");
    var files = fs.readdirSync(resource);
    for (var i = 0; i < files.length; i++) {
        if (/^\./.test(files[i])) {
            continue;
        }
        var stat = fs.statSync(path.join(config.root, current, files[i]));
        href = files[i];
        if (stat.isFile()) {
            href = current + href;
        } else if (stat.isDirectory() && href != "/") {
            href = current + href + "/";
        }
        socket.write("<tr><td><a href='" + href + "'>" + files[i] + "</a></td><td>" + stat.size + " bytes</td><td>" + formatDate(stat.mtime, "yyyy-MM-dd HH:mm:ss") + "</td></tr>");
    };
    socket.write("</tbody></table></body></html>");
}

function resp_success(socket, resource) {
    socket.write("HTTP/1.0 200 OK\n");
    socket.write("Content-Type: " + response.mime + ";charset: UTF-8\n");
    socket.write("Date: " + new Date() + "\n");
    socket.write("Server: xyserver\n");
    socket.write("\n");
    fd = fs.openSync(resource, "r");
    var buffer = new Buffer(512);
    while (true) {
        var flag = fs.readSync(fd, buffer, 0, 512, null);
        socket.write(buffer.slice(0, flag));
        if (flag < 512) {
            break;
        }
    }
    fs.close(fd);
}

function resp_error(socket, status, message) {
    socket.write("HTTP/1.0 " + status + " " + message + "\n");
    socket.write("Content-Type: " + MIME.html + ";charset: UTF-8\n");
    socket.write("Date: " + new Date() + "\n");
    socket.write("Server: xyserver\n");
    socket.write("\n");
    socket.write("<html><head><title>Http Error</title></head><body><h2>Http Error...</h2><p>errror status:"+status+"</p><pre>error message:"+message+"</pre><hr><i><small>Powered by javaway</i></body></html>");
}
function accept_request(socket) {
    socket.on('data', function (data) {
        var req_msg = data.toString("utf-8");
        parse_headers(req_msg);
        console.log(formatDate(new Date(), "yyyy-MM-dd HH:mm:ss") + " " + request.method + " " + request.uri); //log
        var status = init_response(request);
        if (!status) {
            if (fs.existsSync(response.resource)) {
                var stats = fs.lstatSync(response.resource);
                try {
                    if (stats.isFile()) {
                        resp_success(socket, response.resource);
                    } else if (stats.isDirectory()) {
                        if(fs.existsSync(path.join(response.resource,"index.html"))){
                            resp_success(socket, path.join(response.resource,"index.html"));
                        }else{
                            resp_filelist(socket, response.resource);
                        }
                    } else {
                        resp_error(socket, 500, "Bad Request");
                    }
                } catch (e) {
                    resp_error(socket, 500, "Bad Request");
                }

            } else {
                resp_error(socket, 404, "Not Found");
            }
        } else {
            resp_error(socket, 500, "Bad Request");
        }
        socket.destroy();
    });
    socket.on('error', function (e) {
        console.log(e);
    });
}
function start() {
    var httpServer = net.createServer();
    httpServer.on('connection', function (socket) {
        accept_request(socket);
    });
    httpServer.listen(config.port);
    console.log("http server running in http://127.0.0.1:" + config.port);
    console.log("server start work in :" + config.root);
}

function main() { //parse args
    var argv = process.argv.splice(2);
    if (argv && argv.length != 0) { //user config
        var argstr = argv.join(" ");
        argstr = " " + argstr + " ";
        if (/\s-h\s/.test(argstr)) {
            console.log("use case:\n         node server.js -p8080 -r /home/toor/webapp");
            return;
        }
        if (/\s-p\s*\d{2,5}\s/.test(argstr)) {
            config.port = argstr.match(/\s-p\s*(\d+)\s/)[1];
        }
        if (/\s-r\s+\S+\s/.test(argstr)) {
            config.root = argstr.match(/\s-r\s+(\S+)\s/)[1];
        }
    }
    start();
}
main();
