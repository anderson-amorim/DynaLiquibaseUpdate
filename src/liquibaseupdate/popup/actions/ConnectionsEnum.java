package liquibaseupdate.popup.actions;

public enum ConnectionsEnum {

	DYNADEV_DATACENTER("U:\\Softwares\\Windows\\Eclipse\\plugin\\connection\\dynadev.datacenter.properties"),
	DYNADEV_SINGULAR("U:\\Softwares\\Windows\\Eclipse\\plugin\\connection\\dynadev.singular.properties"),
	RDAPAR_DATACENTER("U:\\Softwares\\Windows\\Eclipse\\plugin\\connection\\rdapar.datacenter.properties"),
	RDAPAR_SINGULAR("U:\\Softwares\\Windows\\Eclipse\\plugin\\connection\\rdapar.singular.properties");

	private String propertyFilePath;

	private ConnectionsEnum(String propertyFilePath) {
		this.propertyFilePath = propertyFilePath;
	}

	public String getPropertyFilePath() {
		return propertyFilePath;
	}

}
