package pdd

import groovyx.net.http.HTTPBuilder

class CommunicationService {

    def getHttpBuilder(String url) {
        def http = new HTTPBuilder(url.toString())
        http.setProxy(System.getProperty("127.0.0.1"), System.getProperty("9090") as Integer, 'http')
        http
    }
}