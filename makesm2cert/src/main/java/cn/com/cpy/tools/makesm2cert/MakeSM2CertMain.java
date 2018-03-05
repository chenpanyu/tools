package cn.com.cpy.tools.makesm2cert;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.bc.BcX509v3CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.test.TestRandomBigInteger;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

public class MakeSM2CertMain {

    public static void main(String[] args) {
        BigInteger SM2_ECC_P = new BigInteger("8542D69E4C044F18E8B92435BF6FF7DE457283915C45517D722EDB8B08F1DFC3", 16);
        BigInteger SM2_ECC_A = new BigInteger("787968B4FA32C3FD2417842E73BBFEFF2F3C848B6831D7E0EC65228B3937E498", 16);
        BigInteger SM2_ECC_B = new BigInteger("63E4C6D3B23B0C849CF84241484BFE48F61D59A5B16BA06E6E12D1DA27C5249A", 16);
        BigInteger SM2_ECC_N = new BigInteger("8542D69E4C044F18E8B92435BF6FF7DD297720630485628D5AE74EE7C32E79B7", 16);
        BigInteger SM2_ECC_GX = new BigInteger("421DEBD61B62EAB6746434EBC3CC315E32220B3BADD50BDC4C4E6C147FEDD43D", 16);
        BigInteger SM2_ECC_GY = new BigInteger("0680512BCBB42C07D47349D2153B70C4E5D7FDFCBFA36EA1A85841B9E46E09A2", 16);

        ECCurve curve = new ECCurve.Fp(SM2_ECC_P, SM2_ECC_A, SM2_ECC_B);

        ECPoint g = curve.createPoint(SM2_ECC_GX, SM2_ECC_GY);
        ECDomainParameters domainParams = new ECDomainParameters(curve, g, SM2_ECC_N);

        ECParameterSpec eccSpec = new ECParameterSpec(curve, g, SM2_ECC_N);

        ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();

        ECKeyGenerationParameters aKeyGenParams = new ECKeyGenerationParameters(domainParams, new TestRandomBigInteger("1649AB77A00637BD5E2EFE283FBF353534AA7F7CB89463F208DDBC2920BB0DA0", 16));

        keyPairGenerator.init(aKeyGenParams);

        AsymmetricCipherKeyPair aKp = keyPairGenerator.generateKeyPair();

        ECPublicKeyParameters aPub = (ECPublicKeyParameters) aKp.getPublic();
        ECPrivateKeyParameters aPriv = (ECPrivateKeyParameters) aKp.getPrivate();

        // 证书的名称
        String fileName = "root" + new Date().getTime() / 1000;
        String path = "d:\\";
        String rootCertPath = path + fileName + ".cer";
        try {
            org.bouncycastle.jce.provider.BouncyCastleProvider bouncyCastleProvider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
            Security.addProvider(bouncyCastleProvider);
            bouncyCastleProvider.setParameter(ConfigurableProvider.THREAD_LOCAL_EC_IMPLICITLY_CA, eccSpec);
            BCECPublicKey bcecPublicKey = new BCECPublicKey("1.2.156.10197.1.501", aPub, eccSpec, bouncyCastleProvider.CONFIGURATION);
            BCECPrivateKey bcecPrivateKey = new BCECPrivateKey("1.2.156.10197.1.501", aPriv, bcecPublicKey, eccSpec, bouncyCastleProvider.CONFIGURATION);
            KeyPair kp = new KeyPair(bcecPublicKey, bcecPrivateKey);
            System.out.println("=====公钥算法=====" + kp.getPublic().getAlgorithm());

            // 新方式
            X500NameBuilder x500NameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
            x500NameBuilder.addRDN(BCStyle.C, "AU");
            x500NameBuilder.addRDN(BCStyle.ST, "Victoria");
            x500NameBuilder.addRDN(BCStyle.L, "South Melbourne");
            x500NameBuilder.addRDN(BCStyle.O, "The Legion of the Bouncy Castle");
            x500NameBuilder.addRDN(BCStyle.OU, "Webserver Team");
            x500NameBuilder.addRDN(BCStyle.CN, "www2.connect4.com.au");
            x500NameBuilder.addRDN(BCStyle.E, "webmaster@connect4.com.au");
            X500Name name = x500NameBuilder.build();

            BcX509v3CertificateBuilder bcX509v3CertificateBuilder = new BcX509v3CertificateBuilder(name, BigInteger.valueOf(System.currentTimeMillis()), new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30), new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30), name, aPub);
//            SM2SignerBuilder signerBuilder = new SM2SignerBuilder("111111111111".getBytes(), new TestRandomBigInteger("1649AB77A00637BD5E2EFE283FBF353534AA7F7CB89463F208DDBC2920BB0DA0", 16));
            SM2SignerBuilder signerBuilder = new SM2SignerBuilder(null,new TestRandomBigInteger("1649AB77A00637BD5E2EFE283FBF353534AA7F7CB89463F208DDBC2920BB0DA0", 16));
            X509CertificateHolder holder = bcX509v3CertificateBuilder.build(signerBuilder.build(aPriv));
            CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
            InputStream inputStream = new ByteArrayInputStream(holder.getEncoded());
            X509Certificate rootCert = (X509Certificate) cf.generateCertificate(inputStream);
            inputStream.close();
            rootCert.verify(kp.getPublic(), "BC");
            System.out.println(rootCert.toString());
            FileOutputStream outputStream = new FileOutputStream(rootCertPath);
            outputStream.write(rootCert.getEncoded());
            outputStream.close();

//            holder = new X509CertificateHolder(rootCert.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
