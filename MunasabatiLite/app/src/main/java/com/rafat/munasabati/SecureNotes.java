package com.rafat.munasabati;

import android.content.Context;import android.security.keystore.KeyGenParameterSpec;import android.security.keystore.KeyProperties;import android.util.Base64;import java.nio.charset.StandardCharsets;import java.security.KeyStore;import javax.crypto.*;import javax.crypto.spec.GCMParameterSpec;

/** Optional device-bound AES-GCM encrypted notes. Sensitive notes never enter event sync/export payloads. */
public final class SecureNotes{
 private static final String ALIAS="munasabati_secure_notes_v1",P="munasabati_secure_notes";
 private static SecretKey key()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);java.security.Key k=ks.getKey(ALIAS,null);if(k instanceof SecretKey)return(SecretKey)k;KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());return g.generateKey();}
 public static boolean has(Context c,long id){return c.getSharedPreferences(P,0).contains(String.valueOf(id));}
 public static boolean put(Context c,long id,String text){try{Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(Cipher.ENCRYPT_MODE,key());byte[] ct=x.doFinal((text==null?"":text).getBytes(StandardCharsets.UTF_8)),iv=x.getIV(),all=new byte[iv.length+ct.length];System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(ct,0,all,iv.length,ct.length);c.getSharedPreferences(P,0).edit().putString(String.valueOf(id),Base64.encodeToString(all,Base64.NO_WRAP)).apply();return true;}catch(Exception e){V4Log.error(c,"secure-note",e.toString());return false;}}
 public static String get(Context c,long id){try{String raw=c.getSharedPreferences(P,0).getString(String.valueOf(id),"");if(raw.isEmpty())return"";byte[] all=Base64.decode(raw,Base64.NO_WRAP);if(all.length<13)return"";byte[] iv=new byte[12],ct=new byte[all.length-12];System.arraycopy(all,0,iv,0,12);System.arraycopy(all,12,ct,0,ct.length);Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));return new String(x.doFinal(ct),StandardCharsets.UTF_8);}catch(Exception e){V4Log.error(c,"secure-note",e.toString());return"";}}
 public static void remove(Context c,long id){c.getSharedPreferences(P,0).edit().remove(String.valueOf(id)).apply();}
 private SecureNotes(){}
}
