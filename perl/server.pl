#!/usr/bin/env perl
#tcp_socket_server.pl

use warnings;
use Socket;
use Cwd;
use POSIX qw(strftime);
use File::Spec;
my $port = 8080;     #port
my $root = getcwd;
my %request;         #save headers
my $mime;
my %mime = (
    "text" => "text/plain",
    "html" => "text/html",
    "css"  => "text/css",
    "js"   => "application/javascript",
    "json" => "application/json"
);
my $quit = 0;
$SIG{INT} = $SIG{TERM} = sub {
    $quit++;
    exit(0);
};

sub main {
    my $argstr = join( " ", @ARGV );    #server -p8080 -r /home/toor
    $argstr = " $argstr ";
    if($argstr =~ /\s-h\s\s/ ){
        print "usage:\n";
        print "      perl server.pl -p8080 -r /home/toor/webapp\n";
    }
    if ( $argstr =~ /\s-p\s?\d{2,5}\s/ ) {
        ( $port = $argstr ) =~ s/\s-p\s*?(\d+)\s/$1/;
    }
    if ( $argstr =~ /\s-r\s?\S+\s/ ) {
        ( $root = $argstr ) =~ s/\s-r\s+(\S+)\s/$1/;
    }
    socket( server_socket, AF_INET, SOCK_STREAM, getprotobyname('tcp') )
        or die "Socket $!\n";
    setsockopt( server_socket, SOL_SOCKET, SO_REUSEADDR, 1 )
        or die "Can't set SO_REUSADDR: $!";
    my $my_addr = sockaddr_in( $port, INADDR_ANY );

    bind( server_socket, $my_addr ) or die "Bind $!\n";

    listen( server_socket, 5 ) || die "Listen $!\n";

    print "http server start in http://127.0.0.1:/$port\n";
    while ( !$quit ) {
        accept( client_socket, server_socket ) || die "Accept $!\n";
        defined( $pid = fork ) || die "Fork: $!\n";
        if ( $pid == 0 ) {
            &accept_request(client_socket);
            exit(0);
        }
        else {
            close(client_socket);
        }
    }

}

sub accept_request {    # handle a request
                                      # my $socket = shift;
    &parse_headers(client_socket);    #parse
    my $uri = $request{'uri'};

    # my $now = `date`;
    # $now =~ s/\n//;
    $now = strftime( "%Y-%m-%d %H:%M:%S", localtime );
    print "$now $request{'method'} $uri\n";
    $uri =~ s/(\?.*)//;
    $uri .= "index.html" if ( $uri =~ /\/$/ );
    if ( $uri =~ /\w+\.html$/ ) {
        $mime = $mime{'html'};
    }
    elsif ( $uri =~ /\w+\.css/ ) {
        $mime = $mime{"css"};
    }
    elsif ( $uri =~ /\w+\.js/ ) {
        $mime = $mime{"js"};
    }
    elsif ( $uri =~ /\w+\.json/ ) {
        $mime = $mime{"json"};
    }
    elsif ( $uri =~ /\w+\.do/ ) {
        $mime = $mime{"json"};
        my $prefix;
        my $suffix = $uri;
        my $refer  = $request{'$Referer'};
        if ( $refer && $refer =~ /htmls(\/.*\/)\w+\.html/ ) {
            $prefix = "/data$1";
            $suffix =~ s/\/(\w+)\.do/$1.json/;
            $uri = "$prefix$suffix";
        }
        else {
            resp_error( 500, "Bad Request" );
            close(client_socket);
            exit(1);
        }

    }
    else {
        $mime = "text/html";
    }
    my $filename = File::Spec->catfile( $root, $uri );
    if ( -e -f $filename ) {
        send_success($filename);
    }
    else {
        resp_error( 404, "Not Found" );
    }
    close(client_socket);
}

sub parse_headers {

    # my ($socket) = @_;    #client socket
    my $content = "";
    while (1) {
        my $buffer;
        my $flag = sysread( client_socket, $buffer, 1024 );
        $content .= $buffer;
        last if ( $flag < 1024 );
    }
    if ( $content =~ m/^(.*)\s(\/.*)\s(HTTP\/\d\.\d)/ ) {
        $request{'method'}   = $1;
        $request{'uri'}      = $2;
        $request{'protocol'} = $3;
    }
    my @header = split( /\n/, $content );
    foreach (@header) {
        if (/^([^()<>\@,;:\\"\/\[\]?={} \t]+):\s*(.*)/i) {
            $request{$1} = $2;
        }
    }
}

sub resp_error {    #status, message
    my ( $status, $error ) = @_;
    print client_socket "HTTP/1.0 $status $error\n";
    print client_socket "Content-Type: text/html;charset: UTF-8\n";
    print client_socket "Date: $now\n";
    print client_socket "Server: xyserver\n";
    print client_socket "\n";
}

sub send_success {
    my $filename = shift;
    print client_socket "HTTP/1.0 200 OK\n";
    print client_socket "Content-Type: $mime;charset: UTF-8\n";
    print client_socket "Date: $now\n";
    print client_socket "Server: xyserver\n";
    print client_socket "\n";
    open FILE, "<$filename"
        or die "cannot open $filename:$!";
    foreach (<FILE>) {
        print client_socket $_;
    }
}

main();
