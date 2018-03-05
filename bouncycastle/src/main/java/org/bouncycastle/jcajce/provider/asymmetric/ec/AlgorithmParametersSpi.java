package org.bouncycastle.jcajce.provider.asymmetric.ec;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSASSAPSSparams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.bouncycastle.jcajce.spec.SM2ParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.math.ec.ECCurve;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.spec.*;
import java.util.Enumeration;

public class AlgorithmParametersSpi
    extends java.security.AlgorithmParametersSpi
{
    private ECParameterSpec ecParameterSpec;
    private String curveName;

    protected boolean isASN1FormatString(String format)
    {
        return format == null || format.equals("ASN.1");
    }

    @Override
    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec)
        throws InvalidParameterSpecException
    {
        if (algorithmParameterSpec instanceof ECGenParameterSpec)
        {
            ECGenParameterSpec ecGenParameterSpec = (ECGenParameterSpec)algorithmParameterSpec;
            X9ECParameters params = ECUtils.getDomainParametersFromGenSpec(ecGenParameterSpec);

            if (params == null)
            {
                throw new InvalidParameterSpecException("EC curve name not recognized: " + ecGenParameterSpec.getName());
            }
            curveName = ecGenParameterSpec.getName();
            ecParameterSpec = EC5Util.convertToSpec(params);
        }
        else if (algorithmParameterSpec instanceof ECParameterSpec)
        {
            if (algorithmParameterSpec instanceof ECNamedCurveSpec)
            {
                curveName = ((ECNamedCurveSpec)algorithmParameterSpec).getName();
            }
            else
            {
                curveName = null;
            }
            ecParameterSpec = (ECParameterSpec)algorithmParameterSpec;
        }
        else
        {
            throw new InvalidParameterSpecException("AlgorithmParameterSpec class not recognized: " + algorithmParameterSpec.getClass().getName());
        }
    }

    @Override
    protected void engineInit(byte[] bytes)
        throws IOException
    {
        engineInit(bytes, "ASN.1");
    }

    @Override
    protected void engineInit(byte[] bytes, String format)
        throws IOException
    {
        if (isASN1FormatString(format))
        {
            X962Parameters params = X962Parameters.getInstance(bytes);

            ECCurve curve = EC5Util.getCurve(BouncyCastleProvider.CONFIGURATION, params);

            if (params.isNamedCurve())
            {
                ASN1ObjectIdentifier curveId = ASN1ObjectIdentifier.getInstance(params.getParameters());

                curveName = ECNamedCurveTable.getName(curveId);
                if (curveName == null)
                {
                    curveName = curveId.getId();
                }
            }

            ecParameterSpec = EC5Util.convertToSpec(params, curve);
        }
        else
        {
            throw new IOException("Unknown encoded parameters format in AlgorithmParameters object: " + format);
        }
    }

    @Override
    protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(Class<T> paramSpec)
        throws InvalidParameterSpecException
    {
        if (ECParameterSpec.class.isAssignableFrom(paramSpec) || paramSpec == AlgorithmParameterSpec.class)
        {
            return (T)ecParameterSpec;
        }
        else if (ECGenParameterSpec.class.isAssignableFrom(paramSpec))
        {
            if (curveName != null)
            {
                ASN1ObjectIdentifier namedCurveOid = ECUtil.getNamedCurveOid(curveName);

                if (namedCurveOid != null)
                {
                    return (T)new ECGenParameterSpec(namedCurveOid.getId());
                }
                return (T)new ECGenParameterSpec(curveName);
            }
            else
            {
                ASN1ObjectIdentifier namedCurveOid = ECUtil.getNamedCurveOid(EC5Util.convertSpec(ecParameterSpec, false));

                if (namedCurveOid != null)
                {
                    return (T)new ECGenParameterSpec(namedCurveOid.getId());
                }
            }
        }
        throw new InvalidParameterSpecException("EC AlgorithmParameters cannot convert to " + paramSpec.getName());
    }

    @Override
    protected byte[] engineGetEncoded()
        throws IOException
    {
        return engineGetEncoded("ASN.1");
    }

    @Override
    protected byte[] engineGetEncoded(String format)
        throws IOException
    {
        if (isASN1FormatString(format))
        {
            X962Parameters params;

            if (ecParameterSpec == null)     // implicitly CA
            {
                params = new X962Parameters(DERNull.INSTANCE);
            }
            else if (curveName != null)
            {
                params = new X962Parameters(ECUtil.getNamedCurveOid(curveName));
            }
            else
            {
                org.bouncycastle.jce.spec.ECParameterSpec ecSpec = EC5Util.convertSpec(ecParameterSpec, false);
                X9ECParameters ecP = new X9ECParameters(
                    ecSpec.getCurve(),
                    ecSpec.getG(),
                    ecSpec.getN(),
                    ecSpec.getH(),
                    ecSpec.getSeed());

                params = new X962Parameters(ecP);
            }

            return params.getEncoded();
        }

        throw new IOException("Unknown parameters format in AlgorithmParameters object: " + format);
    }

    @Override
    protected String engineToString()
    {
        return "EC AlgorithmParameters ";
    }

    public static class sm3WithSM2 extends AlgorithmParametersSpi {
        SM2ParameterSpec currentSpec;

        protected void engineInit(AlgorithmParameterSpec paramSpec) throws InvalidParameterSpecException {
            if (!(paramSpec instanceof SM2ParameterSpec)) {
                throw new InvalidParameterSpecException(
                        "SM2ParameterSpec required to initialise an SM3WITHSM2 algorithm parameters object");
            }

            this.currentSpec = (SM2ParameterSpec) paramSpec;
        }

        protected void engineInit(byte[] params) throws IOException {
            DEROctetString der = (DEROctetString) DEROctetString.fromByteArray(params);
            currentSpec = new SM2ParameterSpec(der.getOctets());
        }

        protected void engineInit(byte[] params, String format) throws IOException {
            if (isASN1FormatString(format) || format.equalsIgnoreCase("X.509")) {
                engineInit(params);
            } else {
                throw new IOException("Unknown parameter format " + format);
            }
        }

        protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(Class<T> paramSpec)
                throws InvalidParameterSpecException
        {
            if (SM2ParameterSpec.class.isAssignableFrom(paramSpec) || paramSpec == AlgorithmParameterSpec.class)
            {
                return (T)currentSpec;
            }
            throw new InvalidParameterSpecException("SM3WITHSM2 AlgorithmParameters cannot convert to " + paramSpec.getName());
        }

        protected String engineToString() {
            return "SM3WITHSM2 AlgorithmParameters";
        }
    }
}
