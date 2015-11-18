package pdd
import com.amazonaws.services.s3.model.AccessControlList
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.BucketPolicy
import com.amazonaws.services.s3.model.PutObjectResult
import grails.test.spock.IntegrationSpec
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.HttpResponse
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.joda.time.DateTime
import org.springframework.core.io.ClassPathResource

import static groovyx.net.http.Method.GET

class AmazonS3ServiceIntegrationSpec extends IntegrationSpec {

    static CommunicationService communicationService
    static GrailsApplication grailsApplication
    static AmazonS3Service amazonS3Service

    def setupSpec() {
        amazonS3Service.creerBucketSiInexistante()
    }

    def cleanupSpec() {
        amazonS3Service.supprimerBucketSiExiste()
    }

    /**
     * http://docs.aws.amazon.com/fr_fr/AmazonS3/latest/dev/access-control-overview.html :
     *      By default, all Amazon S3 resources are private. Only a resource owner can access the resource.
     *      The resource owner refers to the AWS account that creates the resource
     *
     * http://docs.aws.amazon.com/fr_fr/AmazonS3/latest/dev/acl-overview.html :
     *      When you create a bucket or an object, Amazon S3 creates a default ACL that grants the resource owner FULL CONTROL over the resource as shown
     *      in the following sample bucket ACL (the default object ACL has the same structure).
     */
    void "creer bucket et s'assurer que seul le proprietaire a les droits"(){
        given:
        def client = amazonS3Service.client
        def bucket = grailsApplication.config.aws.s3.bucket

        when:
        BucketPolicy policy = client.getBucketPolicy(bucket)
        AccessControlList acl = client.getBucketAcl(bucket)

        then:
        policy.policyText == null
        acl.grants.size() == 1
        acl.grants[0].permission.toString() == "FULL_CONTROL"
//        acl.owner.displayName == grailsApplication.config.aws.s3.ownerDisplayName
    }

//    void "lister buckets et s'assurer que seul le propriÃ©taire a les droits"(){
//        given:
//        def client = amazonS3Service.client
//
//        when:
//        def buckets = client.listBuckets();
//
//        then:
//        buckets.each {
//            client.getBucketPolicy(it.name).policyText == null
//            AccessControlList acl = client.getBucketAcl(it.name)
//            acl.grants.size() == 1
//            acl.grants[0].permission.toString() == "FULL_CONTROL"
//            acl.owner.displayName == grailsApplication.config.aws.s3.ownerDisplayName
//        };
//    }


    void "sauvegarder un document et verifier qu'on peut le recuperer"(){
        given:
        def key = amazonS3Service.genererIdentifiant()
        File fileToS3 = new ClassPathResource('/pdd/sample.pdf').getFile()

        when: // on créer un fichier et qu'on le sauvegarde sur S3
        PutObjectResult document = amazonS3Service.sauvegarderDocument(key,fileToS3);

        then: // alors signature md5 ok et droit restreint
        document.getETag() == DigestUtils.md5Hex(new FileInputStream(fileToS3))

        expect: // quand on le récupère
        amazonS3Service.recupererDocument(key) != null

    }

    void "suppression un document et verification qu'il n'existe plus"(){
        setup: // creation du fichier
        def key = amazonS3Service.genererIdentifiant()
        assert amazonS3Service.sauvegarderDocument(key,new ClassPathResource('/pdd/sample.pdf').getFile());

        when: // on le supprime
        amazonS3Service.supprimerDocument(key);

        then: // on vÃ©rifie qu'il n'existe plus
        try {
            amazonS3Service.recupererDocument(key);
        }catch(AmazonS3Exception s3e){
            assert s3e.message.contains("The specified key does not exist")
        }
    }


    void "sauvegarde d'un document et generer un lien public temporaire 5 secondes"(){
        setup:// on sauvegarde un fichier
        def key = amazonS3Service.genererIdentifiant()
        assert amazonS3Service.sauvegarderDocument(key,new ClassPathResource('/pdd/sample.pdf').getFile());

        when:// on le rend public 5 secondes
        URL url = amazonS3Service.genererUrlTemporaire(key,new DateTime(new Date()).plusSeconds(5).toDate())

        then:// on teste qu'il est accessible
        def http = communicationService.getHttpBuilder(url.toString())
        http.request(GET,"application/pdf"){req ->
            response.success = { HttpResponse reponse ->
                assert true
            }
            response.failure = { HttpResponse reponse ->
                assert false
            }
        } == null

        when:// on attend 10s
        Thread.sleep(10000)

        then:// le fichier n'est plus accessible
        http.request(GET,"application/pdf"){req ->
            response.success = { HttpResponse reponse ->
                assert false
            }
            response.failure = { HttpResponse reponse ->
                assert reponse.statusLine.statusCode == 403
            }
        } == null
    }

}
