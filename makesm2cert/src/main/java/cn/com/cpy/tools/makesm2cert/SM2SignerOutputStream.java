package cn.com.cpy.tools.makesm2cert;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by chenpanyu on 2018/1/29.
 */
public class SM2SignerOutputStream
        extends OutputStream {
    private Signer sig;

    SM2SignerOutputStream(Signer sig) {
        this.sig = sig;
    }

    public void write(byte[] bytes, int off, int len)
            throws IOException {
        sig.update(bytes, off, len);
    }

    public void write(byte[] bytes)
            throws IOException {
        sig.update(bytes, 0, bytes.length);
    }

    public void write(int b)
            throws IOException {
        sig.update((byte) b);
    }

    byte[] getSignature()
            throws CryptoException {
        return sig.generateSignature();
    }

    boolean verify(byte[] expected) {
        return sig.verifySignature(expected);
    }
}
