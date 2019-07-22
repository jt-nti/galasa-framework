package dev.voras.framework.internal.creds;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.spec.SecretKeySpec;

import dev.voras.ICredentials;
import dev.voras.framework.spi.FrameworkPropertyFile;
import dev.voras.framework.spi.IConfigurationPropertyStoreService;
import dev.voras.framework.spi.IFramework;
import dev.voras.framework.spi.creds.CredentialsException;
import dev.voras.framework.spi.creds.CredentialsToken;
import dev.voras.framework.spi.creds.CredentialsUsername;
import dev.voras.framework.spi.creds.CredentialsUsernamePassword;
import dev.voras.framework.spi.creds.CredentialsUsernameToken;
import dev.voras.framework.spi.creds.ICredentialsStore;

/**
 *  <p>This class is used to retrieve credentials stored locally, whether they are encrypted or not</p>
 * 
 * @author Bruce Abbott
 */

public class FileCredentialsStore implements ICredentialsStore {
	private final FrameworkPropertyFile fpf;
	private final SecretKeySpec key;
	private final IFramework framework;
	private final IConfigurationPropertyStoreService cpsService;

	public FileCredentialsStore(URI file, IFramework framework) throws CredentialsException {
		try {
			this.framework = framework;
			cpsService = this.framework.getConfigurationPropertyService("secure");         
			fpf = new FrameworkPropertyFile(file);
			String encryptionKey = cpsService.getProperty("credentials.file", "encryption.key");
			if (encryptionKey != null) {
				key = createKey(encryptionKey);
			} else {
				key = null;
			}
		} catch (Exception e) {
			throw new CredentialsException("Unable to initialise the credentials store", e);
		}
	}

	/**
	 * <p>This method is used to retrieve credentials as an appropriate object</p>
	 * 
	 * @param String credentialsId
	 * @throws CredentialsStoreException
	 */
	@Override
	public ICredentials getCredentials(String credentialsId) throws CredentialsException {
		String token = fpf.get("secure.credentials." + credentialsId + ".token");
		if (token != null) {
			String username = fpf.get("secure.credentials." + credentialsId + ".username");

			if (username != null) {
				return new CredentialsUsernameToken(key, username, token);       
			}
			return new CredentialsToken(key, token);       
		}

		String username = fpf.get("secure.credentials." + credentialsId + ".username");
		String password = fpf.get("secure.credentials." + credentialsId + ".password");

		if (username == null) {
			return null;
		}

		if (password == null) {
			return new CredentialsUsername(key, username);
		}

		return new CredentialsUsernamePassword(key, username, password);
	}

	private static SecretKeySpec createKey(String secret) throws UnsupportedEncodingException, NoSuchAlgorithmException {	
		byte[] key = secret.getBytes("UTF-8");
		MessageDigest sha = MessageDigest.getInstance("SHA-256");
		key = sha.digest(key);
		return new SecretKeySpec(key, "AES");
	}

}