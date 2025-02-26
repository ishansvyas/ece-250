import java.util.BitSet;
import java.util.Scanner;
import java.io.File;
// input to run: java cachesim tracefile 1024 4 32

// ERROR ON LOAD MISS -> loaded data is wrong? only outputs 0

public class cachesim {
    //Data
    public static String store = "store";
    public static String load = "load";
    public static String replacementString = "";
    public static int cache_size_kB = 0;
    public static int cache_associativity = 0;
    public static int cache_block_size = 0;
    public static int cache_sets = 0;
    public static String tester_cb = "";

    //Memory
    public static block[][] cache_l1;
    public static byte[] main_memory;

    public static class block {
        byte valid_bit;
        byte dirty_bit;
        int tag;
        String data;
        block(byte valid, byte dirty, int tag, String data) {valid_bit=valid;dirty_bit=dirty;this.tag=tag;this.data=data;}
    }

    public static void main(String[] args) {
        /* READ COMMAND LINE INPUTS */
        // ./cachesim <trace-file> <cache-size-kB> <associativity> <block-size>
        String trace_file = args[0]; // Filename of the memory access trace file.

        String cache_size_kB_String = args[1]; // cache capacity in kB btw 1 and 2048 (is ^2)
        cache_size_kB = Integer.parseInt(cache_size_kB_String);   // convert to int

        String associativity_String = args[2]; // set associativity (# of ways) (is ^2)
        cache_associativity = Integer.parseInt(associativity_String); // convert to int

        String block_size_String = args[3];    // sizeof(cache blocks) in bytes btw 2 and 1024 (is ^2)
        cache_block_size = Integer.parseInt(block_size_String); // convert to int

        /*
        System.out.println(trace_file);
        System.out.println(cache_size_kB);
        System.out.println(cache_associativity);
        System.out.println(cache_block_size);
        */

        /* FORMAT MAIN MEMORY */ 
        main_memory = new byte[power2(24)];
      
        /* FORMAT L1 CACHE */
        cache_sets = power2(10+log2(cache_size_kB) - log2(cache_associativity) - log2(cache_block_size));
        cache_l1 = new block[cache_sets][cache_associativity]; 

        /* READ AND USE TRACE FILE */
        // creates scanner that scans for text input from user
        File file = null;
        Scanner tfScanner = null;
        try {
            file = new File(trace_file);
            tfScanner = new Scanner(file); 
        }
        catch (Exception e) {e.printStackTrace();}
        String[] line_args;
        int bytes=0; int l1_index=0;
        String address=null; String data=null; String rep_before=null; String rep_after=null;
        boolean hit=false;

        while (tfScanner.hasNextLine()) {
            String line = tfScanner.nextLine();
            line_args = line.split(" ");

            if (line_args[0].equals(store)) {
                // insn = store
                //parse input data
                bytes = Integer.parseInt(line_args[2]);
                data = line_args[3];
                address = line_args[1].substring(2);
                
                l1_index = l1_hit(address);
                if (l1_index>=0) {
                    hit_update_lru(get_set_index(address),l1_index);
                    hit=true;
                }
                else {
                    cache_into_l1(address);
                    hit=false;
                }
                if (!replacementString.equals("")) {System.out.println(replacementString);replacementString="";}
                System.out.printf(store);
                System.out.printf(" %s ",line_args[1]);
                if(hit){System.out.printf("hit\n");}else{System.out.printf("miss\n");}
                // change data as needed, update dirty bit (assume index=0)
                cache_l1[get_set_index(address)][0].dirty_bit = (byte)1;
                rep_before = cache_l1[get_set_index(address)][0].data.substring(0,2*get_block_offset(address));
                rep_after = cache_l1[get_set_index(address)][0].data.substring(2*(get_block_offset(address)+bytes));
                cache_l1[get_set_index(address)][0].data = rep_before + data + rep_after;       
            }
            else if (line_args[0].equals(load)) {
                // insn = load
                //parse input data
                bytes = Integer.parseInt(line_args[2]);
                address = line_args[1].substring(2);

                l1_index = l1_hit(address);
                if (l1_index>=0) {
                    hit_update_lru(get_set_index(address),l1_index);
                    hit=true;
                }
                else {
                    cache_into_l1(address);
                    hit=false;
                    //System.out.printf("     %s \n",tester_cb);
                }
                if (!replacementString.equals("")) {System.out.println(replacementString);replacementString="";}
                System.out.printf(load);
                System.out.printf(" %s ",line_args[1]);
                if(hit){System.out.printf("hit ");} else{System.out.printf("miss ");}
                System.out.printf("%s\n",cache_l1[get_set_index(address)][0].data.substring(2*get_block_offset(address),2*(get_block_offset(address)+bytes)));
            }
        }
        tfScanner.close();
        System.exit(0); 
    } 

    // returns index if there a hit in l1 cache, -1 if else
    public static int l1_hit(String s)  {
        int hit = -1;
        int comp = get_address_tag(s);
        for (int i=0; i<cache_associativity;i++) {
            if (cache_l1[get_set_index(s)][i] != null) {
                if (cache_l1[get_set_index(s)][i].tag == comp) {
                    hit = i;
                    break;
                }
            }  
            else {break;}
        }
        return hit;
    }

    // IF NO HIT IN CACHE (cache into l1) -> how to deal with eviction? in function? out of function? 
    // CHECK IN MEMORY <- TO DO!!!!!
    public static void cache_into_l1(String s) {
        block cachable = create_block(s);
        int s_set_index = get_set_index(s);

        // determine if there will be an eviction, and deal with it if so:
        if (cache_l1[s_set_index][cache_associativity-1] != null) {
            // reconstruct address here:
            int block_offset_bits = log2(cache_block_size);
            int set_offset_bits = log2(cache_sets);
            int address = (cache_l1[s_set_index][cache_associativity-1].tag << (set_offset_bits+block_offset_bits)) + (s_set_index << block_offset_bits);
            // change string
            replacementString = replacementString + "replacement 0x" + Integer.toHexString(address) + " ";

            if (cache_l1[s_set_index][cache_associativity-1].dirty_bit == (byte)0) {
                replacementString = replacementString + "clean";
            }
            else if (cache_l1[s_set_index][cache_associativity-1].dirty_bit == (byte)1) {
                replacementString = replacementString + "dirty";
                memory_write_back(cache_l1[s_set_index][cache_associativity-1], s_set_index);
            } 
        }
        // update l1 set
        for (int i=cache_associativity-1;i>0;i--) {
            cache_l1[s_set_index][i] = cache_l1[s_set_index][i-1];
        }
        cache_l1[s_set_index][0] = cachable;
        //System.out.printf("  CACHED THE FOLLOWING DATA: %s\n",cache_l1[s_set_index][0].data);
        // if above, cached only zeros

    }

    // write back (block) in (set) to memory
    public static void memory_write_back(block b, int set) {
        //TESTING LINE: System.out.printf("  DATA: %s\n", b.data);
        String store = b.data;
        int counter = 0;
        int block_offset_bits = log2(cache_block_size);
        int set_offset_bits = log2(cache_sets);
        int address = (b.tag << (set_offset_bits+block_offset_bits)) + (set << block_offset_bits);
        //System.out.printf("    Wrote back to memory at %d, and wrote %s\n",address,store);
        while (counter<cache_block_size) {
            main_memory[address+counter] = (byte)Integer.parseInt(store.substring(0,2),16);
            store = store.substring(2);
            counter++;
        }
    }

    // creates block
    public static block create_block(String s) {
        int block_address = Integer.parseInt(s,16);
        block_address = block_address - get_block_offset(s);
        int bitmask1 = 0xf0;
        int bitmask2 = 0x0f;
        String data = "";
        String append = "";
        // for each byte in block, convert from byte to hex, and from hex to string
        for (int i=0;i<cache_block_size;i++) {
            byte b = main_memory[block_address+i];
            append = Integer.toHexString((b&bitmask1)>>4);
            append = append.concat(Integer.toHexString(b&bitmask2));
            data = data.concat(append);
        }
        //tester_cb = "created block at " + String.format("%d, and wrote: %s",block_address, data); //<-- tester
        block ret = new block((byte)1,(byte)0,get_address_tag(s),data);
        return ret;
    }

    // IF HIT IN CACHE updates the lru with most recent use, given a set and index to input
    public static void hit_update_lru(int set, int index) {
        block lru = cache_l1[set][index];
        for (int i=index;i>0;i--) {
            cache_l1[set][i] = cache_l1[set][i-1];
        }
        cache_l1[set][0] = lru;
    } 

    public static int get_block_offset(String s) {
        // input string s is a 6-digit hex number (0x123456)
        // block offset is the last log2(block_size) digits
        int offset_bits = log2(cache_block_size);
        int bitmask = 0x0001;
        bitmask = (bitmask << offset_bits) - 1;
        // at this point, bitmask is (offset_bits) number of 1s
        //convert String s into hex number
        int hex_s = Integer.parseInt(s,16);
        bitmask = hex_s & bitmask;
        //return just the block offset; no leading zeros?
        return bitmask;
    }

    public static int get_set_index(String s) {
        int block_offset_bits = log2(cache_block_size);
        int set_offset_bits = log2(cache_sets);
        int bitmask_block = 0x0001;
        int bitmask_set = 0x0001;
        bitmask_block = (bitmask_block << block_offset_bits) - 1;
        bitmask_set = (bitmask_set << (block_offset_bits+set_offset_bits)) - 1;
        int bitmask = bitmask_set - bitmask_block;
        //convert String s into hex number
        int hex_s = Integer.parseInt(s,16);
        bitmask = hex_s & bitmask;

        bitmask = bitmask >> block_offset_bits;
        //return set index
        return bitmask;
    }
    
    public static int get_address_tag(String s) {
        int block_offset_bits = log2(cache_block_size);
        int set_offset_bits = log2(cache_sets);
        int bitmask = 0x0001;
        bitmask = (bitmask << (block_offset_bits+set_offset_bits)) - 1;
        bitmask = ~bitmask;
        int hex_s = Integer.parseInt(s,16);
        bitmask = bitmask & hex_s;
        bitmask = bitmask >> (block_offset_bits + set_offset_bits);
        return bitmask;
    }
        // log2 of int
        public static int log2(int n) {
            int r=0;
            n=n>>1;
            while (n>=1) {
                n=n>>1;
                r++;
            }
            return r;
        }
        // power2 of int, power>=0
        public static int power2(int n) {
            int r=1;
            while (n>0) {
                n--;
                r=r*2;
            }
            return r;
        }
}
