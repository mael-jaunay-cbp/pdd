package pdd

import groovyx.net.http.HTTPBuilder

class CommunicationService {

    def getHttpBuilder(String url) {
        def http = new HTTPBuilder(url.toString())
        http.setProxy("127.0.0.1", 9090, 'http')
        http
    }
}