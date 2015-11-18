package pdd

import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol
import groovyx.net.http.HTTPBuilder

class CommunicationService {

    def getHttpBuilder(String url) {
        def http = new HTTPBuilder(url)
        if (posteDeDev) {
            http.setProxy("127.0.0.1", 9090, 'http')
        }
        http
    }

    ClientConfiguration getClientConfiguration() {
        if (posteDeDev) {
            log.debug "client object storage pour poste de dev"
            return new ClientConfiguration(
                    protocol: Protocol.HTTPS,
                    proxyHost: "localhost",
                    proxyPort: 9090)
        } else {
            return new ClientConfiguration(protocol: Protocol.HTTPS)
        }
    }

    static boolean isPosteDeDev() {
        return InetAddress.getLocalHost().getCanonicalHostName().endsWith("nantes.cabinet-besse.fr")
    }


}