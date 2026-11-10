package team.gothaj.client.via;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.ViaMCP;

public class GothajVia {

	public static void init() {
		try {
			ViaMCP.create();
			ViaMCP.INSTANCE.initAsyncSlider(5, 5, 120, 20);
			System.out.println("via version initialised");
		} catch (Exception e) {
			System.out.println("via version fucked");
		}
	}

}
