package battlecode.communication;

public class Coder{
	// This will convert a 15 bit unsigned input to a 31 bit codeword

	// input   : 15 bits
	// salt    : 15 bits
	// modulus : 31 bits
	// keys    : 31 bits
	static private final long encodeKey = 0x16E114E9L;		
	static private final long decodeKey = 0x19C68883L;	
	static private final long modulus   = 0x77587A95L;
    static private final int  teamID    = 0xA800;
    	
	static public int encode(int input){
		return (int)((((long)(teamID+Math.random()*0x100)*0x8000L+input)*encodeKey) % modulus);
	}

	static public int decode(int input){
		return (int)(((input*decodeKey) % modulus) % 0x8000L);
	}	
	
	static public boolean validate(int input){
	    return (int)((input*decodeKey)%modulus)/0x1000000==0xA8;
	}
}
