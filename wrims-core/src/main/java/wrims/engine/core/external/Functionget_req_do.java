package wrims.engine.core.external;

//import java.io.*;
import java.util.*;

public class Functionget_req_do extends ExternalFunction{

	public Functionget_req_do() {
		
	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param7 = stack.pop();
		Object param6 = stack.pop();
		Object param5 = stack.pop();
		Object param4 = stack.pop();
		Object param3 = stack.pop();
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		float n = ((Number) param7).floatValue();
		float gbeta = ((Number) param6).floatValue();
		float galpha = ((Number) param5).floatValue();
		float Sb = ((Number) param4).floatValue();
		float So = ((Number) param3).floatValue();
		float ECstd = ((Number) param2).floatValue();
		float G_o = ((Number) param1).floatValue();

		float result = get_Req_DO(G_o, ECstd, So, Sb, galpha, gbeta, n);
		stack.push(result);

	}

	private float get_Req_DO(float G_o, float ECstd, float So, float Sb, float galpha, float gbeta, float n) {

		float Qreq, arg2, Gavg,YY,DYDQ,QOLD,QNEW,diff,G99avg;
		float gmtol = 1E-08f;
		float gmdT = 0.08333f;
		int nit;

		// **********************************************************************************
		//  Calculate required average G
		//  check log argument so that it is greater than tolerance, otherwise use tolerance
		arg2 = (ECstd - Sb) / (So - Sb);
		if (arg2 < gmtol) arg2 = gmtol;
//		Gavg = (-LOG(arg2) / galpha)**(1.0 / n);
		Gavg = (float)(Math.pow((-Math.log10(arg2) / galpha), (1.0f / n)));
		// *********************************************************************************
		//  Use Newton-Raphson to calculate required Q given Gavg
		//    where   Gavg = Q + gbeta/gmdT * LN((1 + (Q/G_o-1) * EXP(-Q*gmdT/gbeta))/(Q/G_o))
		//
		//    Newton-Raphson uses   Q new = Q old - Y / (dY/dQ)
		//    where  Y = Q/G_o * EXP(-gmdT*(Q-Gavg)/gbeta)  -  Q/G_o * EXP(-gmdT*Q/gbeta)
		//               +  EXP(-gmdT*Q/gbeta)  -  1.0

		QOLD = (gbeta * (Gavg - G_o)/(G_o * gmdT)) + 0.5f * (Gavg + G_o);  // initial guess
		if (QOLD < 500) QOLD = Gavg;     // In case initial guess of Q < 0

		//!  Check the value of  G99AVE that results from a very low Q of 99 cfs.
		//!  	if 	Go > Gavg and G99avg > Gavg,
		//!       then 	set Qreq = 99, and skip Newton-Raphson step.

		Qreq = 0.0f;
		QNEW = 99.0f;        //  Nominal very low outflow
		if (G_o > Gavg) {
			G99avg = QNEW + (gbeta / gmdT) * (float)Math.log10((1.0f + (QNEW / G_o - 1.0f) 
				* (float)Math.exp(-QNEW * gmdT/gbeta)) / (QNEW / G_o));
	       if (G99avg >= Gavg) Qreq = 99.0f;
		}
		if (Qreq < 0.1) {
			diff=gmtol + 1;
			nit = 0;
			while (diff > gmtol) {
				nit = nit + 1;
				if (nit > 50) {
					// System.out.println("Newton-Raphson error - exceeded 50 iterations");
					System.exit(1);
				}
				YY = (QOLD * (float)Math.exp(-gmdT * (QOLD - Gavg) / gbeta) / G_o - (QOLD / G_o - 1.0f) 
					* (float)Math.exp(-gmdT * QOLD / gbeta) - 1.0f);
				DYDQ = (1.0f / G_o - QOLD * gmdT / (G_o * gbeta)) * (float)Math.exp(-gmdT * (QOLD - Gavg) 
					/ gbeta) - (1.0f / G_o - QOLD * gmdT / (G_o * gbeta) + gmdT / gbeta) 
					* (float)Math.exp(-gmdT * QOLD/gbeta);

				QNEW = QOLD - YY / DYDQ;
				if (QNEW < 0) QNEW = 0;
				diff = Math.abs(QNEW - QOLD);
				if (QNEW < 0 && QOLD < 0) {
					diff = 0;
					QNEW = 0;
				}
				QOLD = QNEW;    // Update QOLD value for next iteration
			}
      		Qreq = QOLD;
		}
		return Qreq;
	}
}
