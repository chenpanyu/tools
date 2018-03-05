package org.bouncycastle.operator.bc.gm;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ParametersWithID;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.RuntimeOperatorException;

import java.io.OutputStream;
import java.security.SecureRandom;

/**
 * Created by chenpanyu on 2018/1/26.
 */
public class SM2SignerBuilder{

    private byte[] userId;
    private SecureRandom random;
    private ASN1ObjectIdentifier algorithm;
    private AlgorithmIdentifier sigAlgId;

    public SM2SignerBuilder(byte[] userId, SecureRandom random) {
        this.userId = userId;
        this.random = random;
        // 1.2.156.10197.1.501
        this.algorithm = new DefaultSignatureAlgorithmIdentifierFinder().find("SM3WITHSM2").getAlgorithm();
    }

    public ContentSigner build(AsymmetricKeyParameter privateKey)
            throws OperatorCreationException {
        final Signer sig = new SM2Signer();

        if (userId != null && random != null) {
            sig.init(true, new ParametersWithID(new ParametersWithRandom(privateKey, random), userId));
            this.sigAlgId = new AlgorithmIdentifier(algorithm, new DEROctetString(userId));
        } else if (userId != null){
            sig.init(true, new ParametersWithID(new ParametersWithRandom(privateKey, new SecureRandom()), userId));
            this.sigAlgId = new AlgorithmIdentifier(algorithm, new DEROctetString(userId));
        } else if (random != null) {
            sig.init(true, new ParametersWithRandom(privateKey, random));
            this.sigAlgId = new AlgorithmIdentifier(algorithm);
        } else {
            sig.init(true, privateKey);
            this.sigAlgId = new AlgorithmIdentifier(algorithm);
        }

        return new ContentSigner() {
            private SM2SignerOutputStream stream = new SM2SignerOutputStream(sig);

            public AlgorithmIdentifier getAlgorithmIdentifier() {
                return sigAlgId;
            }

            public OutputStream getOutputStream() {
                return stream;
            }

            public byte[] getSignature() {
                try {
                    return stream.getSignature();
                } catch (CryptoException e) {
                    throw new RuntimeOperatorException("exception obtaining signature: " + e.getMessage(), e);
                }
            }
        };
    }
}
