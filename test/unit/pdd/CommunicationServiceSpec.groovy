package pdd

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.IgnoreIf
import spock.lang.Specification

class CommunicationServiceSpec extends Specification {

    @IgnoreIf({InetAddress.getLocalHost().getHostName() != "ubuntu-optiplex-7010"})
    void "le pc de dj est un poste de dev"() {
        expect:
        new CommunicationService().isPosteDeDev()
    }
}
