//	!     Last change:  R    10 May 2007
//	! Dan Easton 5/10/2007
//	! This function allocates Article 56 carryover and spills based on Table A entitlement and carryover request.

package wrims.engine.core.external;

import java.util.*;


public class Functiona56_allocation extends ExternalFunction {
	private final boolean DEBUG = false;
	private float[] _allocation;
	public final int ARRAY_SIZE = 31;

	public Functiona56_allocation() {

	}

	public void execute(Stack stack)  {
		int stackSize = stack.size();

		if (stackSize == 2) {
			//values in reverse order:
			Object param2 = stack.pop();
			Object param1 = stack.pop();
	
			//cast parameters to correct types:
			int contractor = ((Number) param2).intValue();
			int calc = ((Number) param1).intValue();
	
			float result = a56_allocation(calc, contractor);
			
			if (DEBUG) {
				System.out.println("*************A56_Allocation call**************");
				System.out.println("result = " + result);
			}

			// push the result on the Stack
			stack.push(new Float(result));
			
		} else if (stackSize == 63) {
			//values in reverse order:
			Object param63 = stack.pop();
			Object param62 = stack.pop();
			Object param61 = stack.pop();
			Object param60 = stack.pop();
			Object param59 = stack.pop();
			Object param58 = stack.pop();
			Object param57 = stack.pop();
			Object param56 = stack.pop();
			Object param55 = stack.pop();
			Object param54 = stack.pop();
			Object param53 = stack.pop();
			Object param52 = stack.pop();
			Object param51 = stack.pop();
			Object param50 = stack.pop();
			Object param49 = stack.pop();
			Object param48 = stack.pop();
			Object param47 = stack.pop();
			Object param46 = stack.pop();
			Object param45 = stack.pop();
			Object param44 = stack.pop();
			Object param43 = stack.pop();
			Object param42 = stack.pop();
			Object param41 = stack.pop();
			Object param40 = stack.pop();
			Object param39 = stack.pop();
			Object param38 = stack.pop();
			Object param37 = stack.pop();
			Object param36 = stack.pop();
			Object param35 = stack.pop();
			Object param34 = stack.pop();
			Object param33 = stack.pop();
			Object param32 = stack.pop();
			Object param31 = stack.pop();
			Object param30 = stack.pop();
			Object param29 = stack.pop();
			Object param28 = stack.pop();
			Object param27 = stack.pop();
			Object param26 = stack.pop();
			Object param25 = stack.pop();
			Object param24 = stack.pop();
			Object param23 = stack.pop();
			Object param22 = stack.pop();
			Object param21 = stack.pop();
			Object param20 = stack.pop();
			Object param19 = stack.pop();
			Object param18 = stack.pop();
			Object param17 = stack.pop();
			Object param16 = stack.pop();
			Object param15 = stack.pop();
			Object param14 = stack.pop();
			Object param13 = stack.pop();
			Object param12 = stack.pop();
			Object param11 = stack.pop();
			Object param10 = stack.pop();
			Object param9 = stack.pop();
			Object param8 = stack.pop();
			Object param7 = stack.pop();
			Object param6 = stack.pop();
			Object param5 = stack.pop();
			Object param4 = stack.pop();
			Object param3 = stack.pop();
			Object param2 = stack.pop();
			Object param1 = stack.pop();
	
			//cast parameters to correct types:
			float req_30 = ((Number) param63).floatValue();
			float ta_30 = ((Number) param62).floatValue();
			float req_29 = ((Number) param61).floatValue();
			float ta_29 = ((Number) param60).floatValue();
			float req_28 = ((Number) param59).floatValue();
			float ta_28 = ((Number) param58).floatValue();
			float req_27 = ((Number) param57).floatValue();
			float ta_27 = ((Number) param56).floatValue();
			float req_26 = ((Number) param55).floatValue();
			float ta_26 = ((Number) param54).floatValue();
			float req_25 = ((Number) param53).floatValue();
			float ta_25 = ((Number) param52).floatValue();
			float req_24 = ((Number) param51).floatValue();
			float ta_24 = ((Number) param50).floatValue();
			float req_23 = ((Number) param49).floatValue();
			float ta_23 = ((Number) param48).floatValue();
			float req_22 = ((Number) param47).floatValue();
			float ta_22 = ((Number) param46).floatValue();
			float req_21 = ((Number) param45).floatValue();
			float ta_21 = ((Number) param44).floatValue();
			float req_20 = ((Number) param43).floatValue();
			float ta_20 = ((Number) param42).floatValue();
			float req_19 = ((Number) param41).floatValue();
			float ta_19 = ((Number) param40).floatValue();
			float req_18 = ((Number) param39).floatValue();
			float ta_18 = ((Number) param38).floatValue();
			float req_17 = ((Number) param37).floatValue();
			float ta_17 = ((Number) param36).floatValue();
			float req_16 = ((Number) param35).floatValue();
			float ta_16 = ((Number) param34).floatValue();
			float req_15 = ((Number) param33).floatValue();
			float ta_15 = ((Number) param32).floatValue();
			float req_14 = ((Number) param31).floatValue();
			float ta_14 = ((Number) param30).floatValue();
			float req_13 = ((Number) param29).floatValue();
			float ta_13 = ((Number) param28).floatValue();
			float req_12 = ((Number) param27).floatValue();
			float ta_12 = ((Number) param26).floatValue();
			float req_11 = ((Number) param25).floatValue();
			float ta_11 = ((Number) param24).floatValue();
			float req_10 = ((Number) param23).floatValue();
			float ta_10 = ((Number) param22).floatValue();
			float req_9 = ((Number) param21).floatValue();
			float ta_9 = ((Number) param20).floatValue();
			float req_8 = ((Number) param19).floatValue();
			float ta_8 = ((Number) param18).floatValue();
			float req_7 = ((Number) param17).floatValue();
			float ta_7 = ((Number) param16).floatValue();
			float req_6 = ((Number) param15).floatValue();
			float ta_6 = ((Number) param14).floatValue();
			float req_5 = ((Number) param13).floatValue();
			float ta_5 = ((Number) param12).floatValue();
			float req_4 = ((Number) param11).floatValue();
			float ta_4 = ((Number) param10).floatValue();
			float req_3 = ((Number) param9).floatValue();
			float ta_3 = ((Number) param8).floatValue();
			float req_2 = ((Number) param7).floatValue();
			float ta_2 = ((Number) param6).floatValue();
			float req_1 = ((Number) param5).floatValue();
			float ta_1 = ((Number) param4).floatValue();
			float spill = ((Number) param3).floatValue();
			int contractor = ((Number) param2).intValue();
			int calc = ((Number) param1).intValue();
			
			if (DEBUG) {
				System.out.println("*************A56_Allocation call**************");
				System.out.println("calc = " + calc);
				System.out.println("contractor = " + contractor);
				System.out.println("spill = " + spill);
				System.out.println("ta_1 = " + ta_1);
				System.out.println("req_1 = " + req_1);
				System.out.println("ta_2 = " + ta_2);
				System.out.println("req_2 = " + req_2);
				System.out.println("ta_3 = " + ta_3);
				System.out.println("req_3 = " + req_3);
				System.out.println("ta_4 = " + ta_4);
				System.out.println("req_4 = " + req_4);
				System.out.println("ta_5 = " + ta_5);
				System.out.println("req_5 = " + req_5);
				System.out.println("ta_6 = " + ta_6);
				System.out.println("req_6 = " + req_6);
				System.out.println("ta_7 = " + ta_7);
				System.out.println("req_7 = " + req_7);
				System.out.println("ta_8 = " + ta_8);
				System.out.println("req_8 = " + req_8);
				System.out.println("ta_9 = " + ta_9);
				System.out.println("req_9 = " + req_9);
				System.out.println("ta_10 = " + ta_10);
				System.out.println("req_10 = " + req_10);
				System.out.println("ta_11 = " + ta_11);
				System.out.println("req_11 = " + req_11);
				System.out.println("ta_12 = " + ta_12);
				System.out.println("req_12 = " + req_12);
				System.out.println("ta_13 = " + ta_13);
				System.out.println("req_13 = " + req_13);
				System.out.println("ta_14 = " + ta_14);
				System.out.println("req_14 = " + req_14);
				System.out.println("ta_15 = " + ta_15);
				System.out.println("req_15 = " + req_15);
				System.out.println("ta_16 = " + ta_16);
				System.out.println("req_16 = " + req_16);
				System.out.println("ta_17 = " + ta_17);
				System.out.println("req_17 = " + req_17);
				System.out.println("ta_18 = " + ta_18);
				System.out.println("req_18 = " + req_18);
				System.out.println("ta_19 = " + ta_19);
				System.out.println("req_19 = " + req_19);
				System.out.println("ta_20 = " + ta_20);
				System.out.println("req_20 = " + req_20);
				System.out.println("ta_21 = " + ta_21);
				System.out.println("req_21 = " + req_21);
				System.out.println("ta_22 = " + ta_22);
				System.out.println("req_22 = " + req_22);
				System.out.println("ta_23 = " + ta_23);
				System.out.println("req_23 = " + req_23);
				System.out.println("ta_24 = " + ta_24);
				System.out.println("req_24 = " + req_24);
				System.out.println("ta_25 = " + ta_25);
				System.out.println("req_25 = " + req_25);
				System.out.println("ta_26 = " + ta_26);
				System.out.println("req_26 = " + req_26);
				System.out.println("ta_27 = " + ta_27);
				System.out.println("req_27 = " + req_27);
				System.out.println("ta_28 = " + ta_28);
				System.out.println("req_28 = " + req_28);
				System.out.println("ta_29 = " + ta_29);
				System.out.println("req_29 = " + req_29);
				System.out.println("ta_30 = " + ta_30);
				System.out.println("req_30 = " + req_30);				
			}
	
			float result = a56_allocation(calc, contractor, spill, ta_1, req_1, ta_2, req_2, ta_3,
				req_3, ta_4, req_4, ta_5, req_5, ta_6, req_6, ta_7, req_7, ta_8, req_8, ta_9, req_9,
				ta_10, req_10, ta_11, req_11, ta_12, req_12, ta_13, req_13, ta_14, req_14, ta_15,
				req_15, ta_16, req_16, ta_17, req_17, ta_18, req_18, ta_19, req_19, ta_20, req_20,
				ta_21, req_21, ta_22, req_22, ta_23, req_23, ta_24, req_24, ta_25, req_25, ta_26,
				req_26, ta_27, req_27, ta_28, req_28, ta_29, req_29, ta_30, req_30);
			
			if (DEBUG) {
				System.out.println("*************A56_Allocation call**************");
				System.out.println("result = " + result);
			}

			// push the result on the Stack
			stack.push(new Float(result));
		}
	}
	
	private float a56_allocation(int calc, int contractor) {
		return _allocation[contractor];
	}

	/**
	 * Table A entitlements, spill, and Article 56 requests are necessary only when allocations
	 * are calculated (calc = 1).  Otherwise, those parameters are optional and you can pull the
	 * previously calculated allocation for a contractor by simply making the following function
	 * call <code>a56_allocation(0, contractor)</code>.
	 */
	private float a56_allocation(int calc, int contractor, float spill, float ta_1,	float req_1,
			float ta_2, float req_2, float ta_3, float req_3, float ta_4, float req_4, float ta_5,
			float req_5, float ta_6,float req_6, float ta_7, float req_7, float ta_8, float req_8,
			float ta_9,	float req_9, float ta_10, float req_10, float ta_11, float req_11,
			float ta_12, float req_12, float ta_13, float req_13, float ta_14, float req_14,
			float ta_15, float req_15, float ta_16, float req_16, float ta_17, float req_17,
			float ta_18, float req_18, float ta_19, float req_19, float ta_20, float req_20,
			float ta_21, float req_21, float ta_22, float req_22, float ta_23, float req_23,
			float ta_24, float req_24, float ta_25, float req_25, float ta_26, float req_26,
			float ta_27, float req_27, float ta_28, float req_28, float ta_29, float req_29,
			float ta_30, float req_30) {


		//REAL,DIMENSION(30) :: allocation, shortage, surplus, ta, req, req_alloc
		// moved allocation to instance variables
		_allocation = new float[ARRAY_SIZE];
		float[] shortage = new float[ARRAY_SIZE];
		float[] surplus = new float[ARRAY_SIZE];
		float[] ta = new float[ARRAY_SIZE];
		float[] req = new float[ARRAY_SIZE];
		float[] req_alloc = new float[ARRAY_SIZE];
		float tot_ta, tot_req, tot_avail, tot_short = 999, tot_surp = 999, adj;
		int i, j, stop;

		//The result of the function is the final allocation fraction for the given contractor.
		float alloc;

		//If calc == 1, then user wants allocations calculated.
		//If calc != 1 (presently setting calc = 0 in wresl calls), bypass if block and pull
		// contractor allocation from previous calculation.
		if (calc==1) {
			//Initialize TableA and Carryover Request Arrays
			ta[1] = ta_1;
			ta[2] = ta_2;
			ta[3] = ta_3;
			ta[4] = ta_4;
			ta[5] = ta_5;
			ta[6] = ta_6;
			ta[7] = ta_7;
			ta[8] = ta_8;
			ta[9] = ta_9;
			ta[10] = ta_10;
			ta[11] = ta_11;
			ta[12] = ta_12;
			ta[13] = ta_13;
			ta[14] = ta_14;
			ta[15] = ta_15;
			ta[16] = ta_16;
			ta[17] = ta_17;
			ta[18] = ta_18;
			ta[19] = ta_19;
			ta[20] = ta_20;
			ta[21] = ta_21;
			ta[22] = ta_22;
			ta[23] = ta_23;
			ta[24] = ta_24;
			ta[25] = ta_25;
			ta[26] = ta_26;
			ta[27] = ta_27;
			ta[28] = ta_28;
			ta[29] = ta_29;
			ta[30] = ta_30;
			req[1] = req_1;
			req[2] = req_2;
			req[3] = req_3;
			req[4] = req_4;
			req[5] = req_5;
			req[6] = req_6;
			req[7] = req_7;
			req[8] = req_8;
			req[9] = req_9;
			req[10] = req_10;
			req[11] = req_11;
			req[12] = req_12;
			req[13] = req_13;
			req[14] = req_14;
			req[15] = req_15;
			req[16] = req_16;
			req[17] = req_17;
			req[18] = req_18;
			req[19] = req_19;
			req[20] = req_20;
			req[21] = req_21;
			req[22] = req_22;
			req[23] = req_23;
			req[24] = req_24;
			req[25] = req_25;
			req[26] = req_26;
			req[27] = req_27;
			req[28] = req_28;
			req[29] = req_29;
			req[30] = req_30;

			//Sum TableA and Carryover Request
			tot_ta = 0;
			tot_req = 0;
			for (i = 1; i <= 30; i++) {
				tot_ta += ta[i];
				tot_req += req[i];
			}

			//Calculate available carryover
			tot_avail = Math.max(0, tot_req - spill);

			//Initialize allocation and req_alloc
			if (tot_avail < 0.005 || tot_ta < 0.005) {
				for (i = 1; i <= 30; ++i)
					_allocation[i] = 0;
			} else {
				for (i = 1; i <= 30; ++i) {
					req_alloc[i] = req[i]/tot_avail;
					_allocation[i] = ta[i]/tot_ta;    //CB fraction of Table A
				}
				
				//Allocate carryover and spills.
				stop = 0;
				j = 0;

				while (tot_surp >= 0.0001 && tot_short >= 0.0001 && j <= 29) { //Check to see if allocation is complete
					++j;			//loop index, j should not exceed 29 since only 30 contractors
					tot_short = 0;		//initialize sum of contractor Article 56 shortages
					tot_surp = 0;		//initialize sum of contractor Article 56 surplus allocation
					tot_ta = 0;			//initialize sum of Table A entitlements of shorted contractors

					//With initial allocation calculate shortages and surplus and new Table A denominator for short contractors
					for (i = 1; i <= 30; ++i) {
						surplus[i] = (float)Math.max(0., _allocation[i] - req_alloc[i]);
						shortage[i] = (float)Math.max(0., req_alloc[i] - _allocation[i]);

						tot_surp += surplus[i];
						tot_short += shortage[i];

						if (shortage[i] > 0.00001)
							tot_ta += ta[i];
					} //end for

					//Adjust allocations by allocating available surplus to shorted contractors based on their Table A entitlements
					for (i = 1; i <= 30; ++i) {
						if (shortage[i] > 0.00001) {
							adj = 0;
							if (tot_ta > 0.0005) {
								adj = tot_surp*ta[i]/tot_ta;
								_allocation[i] += adj;
		        			} 
						}else{
							_allocation[i] = req_alloc[i];
		        		}
		        	} //end for
		        } //end while
			}
		} // end if
		return _allocation[contractor];
	}
}
