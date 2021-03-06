import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;

	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;

	public HuffProcessor() {
		this(0);
	}

	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		int[] counts = readForCounts(in);
		HuffNode start = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(start);
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(start,out);
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
	}
	private int[] readForCounts(BitInputStream in) {
		int[] freq = new int[ALPH_SIZE + 1];
		while(true){
			int character = in.readBits(BITS_PER_WORD);
			//System.out.print(character);
			if(character == -1)
				break;
			freq[character]++;
		}
		freq[PSEUDO_EOF] = 1;
		return freq;
	}
	private HuffNode makeTreeFromCounts(int[] counts) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		//int index = 0;
		for(int i = 0; i < ALPH_SIZE; i++){
			if(counts[i] != 0){
				pq.add(new HuffNode(i, counts[i]));
			}
		}
		pq.add(new HuffNode(PSEUDO_EOF, 0));
		while(pq.size() > 1){
			HuffNode node1 = pq.poll();
			HuffNode node2 = pq.poll();
			pq.add(new HuffNode(-1, node1.myWeight+node2.myWeight, node1, node2));
		}
		HuffNode start = pq.poll();
		return start;
	}
	private String[] makeCodingsFromTree(HuffNode start) {
		String[] encodings = new String[ALPH_SIZE+1];
		codingHelper(start, "", encodings);
		//System.out.println(encodings.length);
		return encodings;
		//figure out what to return from coding helper
	}
	private void codingHelper(HuffNode start, String str, String[] encodings) {
		if(start.myLeft == null && start.myRight == null){
			//System.out.println(start);
			encodings[start.myValue] = str;
			//System.out.println(start.myValue);
			return;
		}
		codingHelper(start.myLeft, str + 0, encodings);
		codingHelper(start.myRight, str + 1, encodings);
	}
	private void writeHeader(HuffNode start, BitOutputStream out) {
		if(start.myLeft == null && start.myRight == null){
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, start.myValue);
			return;
		}
		out.writeBits(1, 0);
		writeHeader(start.myLeft, out);
		writeHeader(start.myRight, out);
	}
	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		while(true){
			int character = in.readBits(BITS_PER_WORD);
			//System.out.println(character);
			if(character == -1)
				break;
			String encodings = codings[character];
			out.writeBits(encodings.length(), Integer.parseInt(encodings, 2));
		}
		String code = codings[PSEUDO_EOF];		
		out.writeBits(code.length(), Integer.parseInt(code,2));
	}
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
	    int bit = in.readBits(BITS_PER_INT);
	    if(bit != HUFF_TREE){
	    	throw new HuffException("illegal header starts with"+in.readBits(BITS_PER_INT));
	    	}
		HuffNode start = readTreeHeader(in);
		readCompressedBits(start, in, out);
		out.close();
	}
	
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		HuffNode node = root;
		while(true){
			if(node.myLeft == null && node.myRight == null){
				if(node.myValue == PSEUDO_EOF) break;
				out.writeBits(BITS_PER_WORD, node.myValue);
				node = root;
			}
			int val = in.readBits(1);
			if(val == -1) throw new HuffException("bad input, no PSEUDO_EOF");
			if(val == 0) node = node.myLeft;
			if(val == 1) node = node.myRight;
		}
		
	}

	private HuffNode readTreeHeader(BitInputStream in) {
		int fBit = in.readBits(1);
		if(fBit == 0){
			return new HuffNode(0,0, readTreeHeader(in),readTreeHeader(in));	
		}
		return new HuffNode(in.readBits(BITS_PER_WORD + 1), 0, null, null);
	}
}


