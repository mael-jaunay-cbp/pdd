package pdd

import com.amazonaws.ClientConfiguration
import com.amazonaws.HttpMethod
import com.amazonaws.Protocol
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.*
import grails.transaction.Transactional

import static java.util.UUID.randomUUID

@Transactional
class AmazonS3Service {

    def grailsApplication
    def communicationService

    String genererIdentifiant() {
        randomUUID().toString()
    }

    AmazonS3Client getClient() {
        // https://forums.aws.amazon.com/thread.jspa?messageID=561113&#561113
        BasicAWSCredentials credentials = new BasicAWSCredentials(
                aws.credentials.accessKey,
                aws.credentials.secretKey)

        AmazonS3 amazonClient = new AmazonS3Client(credentials, communicationService.clientConfiguration)
        amazonClient.setEndpoint(aws.s3.host)
        amazonClient
    }

    private ConfigObject getAws() {
        grailsApplication.config.aws
    }

    private String getBucket() {
        grailsApplication.config.aws.s3.bucket
    }

    byte[] recupererDocument(String objectKey) {
        S3Object object = client.getObject(new GetObjectRequest(bucket, objectKey));
        object.getObjectContent().getBytes()
    }

    void supprimerDocument(String key) {
        client.deleteObject(new DeleteObjectRequest(bucket, key));
    }

    PutObjectResult sauvegarderDocument(String objectKey, File objectDocument) {
        client.putObject(new PutObjectRequest(bucket, objectKey, objectDocument));
    }

    /**
     * CrÃ©e la bucket si elle n'existe pas
     */
    String creerBucketSiInexistante() {
        def s3 = getClient()
        if (!s3.doesBucketExist(bucket)) {
            s3.createBucket(bucket)
        }
        bucket
    }

    /**
     * Supprime la bucket et les fichiers si la bucket existe
     */
    void supprimerBucketSiExiste() {
        def s3 = getClient()
//        DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(bucketName);
//
//        ObjectListing docs = listerDocuments(client, bucketName)
//        multiObjectDeleteRequest.setKeys(
//                docs.objectSummaries.collect {
//                    new DeleteObjectsRequest.KeyVersion(it.key)
//                }
//        );
//        client.deleteObjects(multiObjectDeleteRequest)
        if (s3.doesBucketExist(bucket)) {
            ObjectListing docs = s3.listObjects(bucket)
            docs.objectSummaries.each {
                s3.deleteObject(bucket, it.key)
            }
            s3.deleteBucket(bucket)
        }
    }

    URL genererUrlTemporaire(String key, Date experitation) {
        def urlRequest = new GeneratePresignedUrlRequest(bucket, key);
        urlRequest.setMethod(HttpMethod.GET); // Default.
        urlRequest.setExpiration(experitation);
        client.generatePresignedUrl(urlRequest)
    }

}