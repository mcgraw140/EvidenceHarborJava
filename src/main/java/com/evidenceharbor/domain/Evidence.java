package com.evidenceharbor.domain;

public class Evidence {
    private int id;
    private String barcode;
    private int caseId;
    private int collectedByOfficerId;
    private int collectedFromPersonId;
    private String collectionDate;
    private String specificLocation;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String evidenceType;
    private String description;
    private String status;
    private String storageLocation;

    // Ammunition
    private String ammoCallber;
    private String ammoQuantity;
    private String ammoGrainWeight;
    private String ammoBulletType;
    private String ammoBrand;

    // Biological / DNA
    private String bioSampleType;
    private String bioCollectionMethod;
    private String bioStorageTemp;
    private String bioSuspectName;
    private boolean bioDnaAnalysisRequested;

    // Currency
    private String currencyAmount;
    private String currencyDenominations;
    private String currencySerialNumbers;
    private boolean currencySuspectedCounterfeit;

    // Electronics
    private String elecDeviceType;
    private String elecMake;
    private String elecModel;
    private String elecSerialNumber;
    private boolean elecPasswordProtected;
    private boolean elecDataExtractionRequested;
    private String elecDeviceUsername;
    private String elecDevicePassword;

    // Firearm
    private String firearmMake;
    private String firearmModel;
    private String firearmSerialNumber;
    private String firearmType;
    private String firearmCaliber;
    private boolean firearmReportedStolen;
    private boolean firearmLoadedWhenRecovered;

    // Jewelry
    private String jewelryType;
    private String jewelryMaterial;
    private String jewelryEstimatedValue;
    private String jewelryEngravingOrId;

    // Narcotic Equipment
    private String narcEquipType;
    private String narcEquipDescription;
    private String narcEquipSuspectedResidue;
    private boolean narcEquipFieldTestKitUsed;

    // Narcotics
    private String narcDrugType;
    private String narcNetWeight;
    private String narcForm;
    private String narcPackaging;
    private boolean narcFieldTestPerformed;
    private String narcFieldTestResult;

    // Vehicle
    private String vehicleBodyType;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleYear;
    private String vehicleColor;
    private String vehicleVin;
    private String vehicleLicensePlate;
    private String vehicleLicenseState;
    private boolean vehicleReportedStolen;
    private boolean vehicleImpounded;

    // Weapon
    private String weaponType;
    private String weaponMake;
    private String weaponModel;
    private String weaponSerialNumber;
    private String weaponLength;
    private boolean weaponReportedStolen;

    public Evidence() {}

    // ── Core ──────────────────────────────────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String s) { this.barcode = s; }
    public int getCaseId() { return caseId; }
    public void setCaseId(int id) { this.caseId = id; }
    public int getCollectedByOfficerId() { return collectedByOfficerId; }
    public void setCollectedByOfficerId(int id) { this.collectedByOfficerId = id; }
    public int getCollectedFromPersonId() { return collectedFromPersonId; }
    public void setCollectedFromPersonId(int id) { this.collectedFromPersonId = id; }
    public String getCollectionDate() { return collectionDate; }
    public void setCollectionDate(String s) { this.collectionDate = s; }
    public String getSpecificLocation() { return specificLocation; }
    public void setSpecificLocation(String s) { this.specificLocation = s; }
    public String getAddress() { return address; }
    public void setAddress(String s) { this.address = s; }
    public String getCity() { return city; }
    public void setCity(String s) { this.city = s; }
    public String getState() { return state; }
    public void setState(String s) { this.state = s; }
    public String getZip() { return zip; }
    public void setZip(String s) { this.zip = s; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String s) { this.evidenceType = s; }
    public String getDescription() { return description; }
    public void setDescription(String s) { this.description = s; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String s) { this.storageLocation = s; }

    // ── Ammunition ─────────────────────────────────────────────────────────────
    public String getAmmoCallber() { return ammoCallber; }
    public void setAmmoCallber(String s) { this.ammoCallber = s; }
    public String getAmmoQuantity() { return ammoQuantity; }
    public void setAmmoQuantity(String s) { this.ammoQuantity = s; }
    public String getAmmoGrainWeight() { return ammoGrainWeight; }
    public void setAmmoGrainWeight(String s) { this.ammoGrainWeight = s; }
    public String getAmmoBulletType() { return ammoBulletType; }
    public void setAmmoBulletType(String s) { this.ammoBulletType = s; }
    public String getAmmoBrand() { return ammoBrand; }
    public void setAmmoBrand(String s) { this.ammoBrand = s; }

    // ── Biological / DNA ───────────────────────────────────────────────────────
    public String getBioSampleType() { return bioSampleType; }
    public void setBioSampleType(String s) { this.bioSampleType = s; }
    public String getBioCollectionMethod() { return bioCollectionMethod; }
    public void setBioCollectionMethod(String s) { this.bioCollectionMethod = s; }
    public String getBioStorageTemp() { return bioStorageTemp; }
    public void setBioStorageTemp(String s) { this.bioStorageTemp = s; }
    public String getBioSuspectName() { return bioSuspectName; }
    public void setBioSuspectName(String s) { this.bioSuspectName = s; }
    public boolean isBioDnaAnalysisRequested() { return bioDnaAnalysisRequested; }
    public void setBioDnaAnalysisRequested(boolean b) { this.bioDnaAnalysisRequested = b; }

    // ── Currency ───────────────────────────────────────────────────────────────
    public String getCurrencyAmount() { return currencyAmount; }
    public void setCurrencyAmount(String s) { this.currencyAmount = s; }
    public String getCurrencyDenominations() { return currencyDenominations; }
    public void setCurrencyDenominations(String s) { this.currencyDenominations = s; }
    public String getCurrencySerialNumbers() { return currencySerialNumbers; }
    public void setCurrencySerialNumbers(String s) { this.currencySerialNumbers = s; }
    public boolean isCurrencySuspectedCounterfeit() { return currencySuspectedCounterfeit; }
    public void setCurrencySuspectedCounterfeit(boolean b) { this.currencySuspectedCounterfeit = b; }

    // ── Electronics ────────────────────────────────────────────────────────────
    public String getElecDeviceType() { return elecDeviceType; }
    public void setElecDeviceType(String s) { this.elecDeviceType = s; }
    public String getElecMake() { return elecMake; }
    public void setElecMake(String s) { this.elecMake = s; }
    public String getElecModel() { return elecModel; }
    public void setElecModel(String s) { this.elecModel = s; }
    public String getElecSerialNumber() { return elecSerialNumber; }
    public void setElecSerialNumber(String s) { this.elecSerialNumber = s; }
    public boolean isElecPasswordProtected() { return elecPasswordProtected; }
    public void setElecPasswordProtected(boolean b) { this.elecPasswordProtected = b; }
    public boolean isElecDataExtractionRequested() { return elecDataExtractionRequested; }
    public void setElecDataExtractionRequested(boolean b) { this.elecDataExtractionRequested = b; }
    public String getElecDeviceUsername() { return elecDeviceUsername; }
    public void setElecDeviceUsername(String s) { this.elecDeviceUsername = s; }
    public String getElecDevicePassword() { return elecDevicePassword; }
    public void setElecDevicePassword(String s) { this.elecDevicePassword = s; }

    // ── Firearm ────────────────────────────────────────────────────────────────
    public String getFirearmMake() { return firearmMake; }
    public void setFirearmMake(String s) { this.firearmMake = s; }
    public String getFirearmModel() { return firearmModel; }
    public void setFirearmModel(String s) { this.firearmModel = s; }
    public String getFirearmSerialNumber() { return firearmSerialNumber; }
    public void setFirearmSerialNumber(String s) { this.firearmSerialNumber = s; }
    public String getFirearmType() { return firearmType; }
    public void setFirearmType(String s) { this.firearmType = s; }
    public String getFirearmCaliber() { return firearmCaliber; }
    public void setFirearmCaliber(String s) { this.firearmCaliber = s; }
    public boolean isFirearmReportedStolen() { return firearmReportedStolen; }
    public void setFirearmReportedStolen(boolean b) { this.firearmReportedStolen = b; }
    public boolean isFirearmLoadedWhenRecovered() { return firearmLoadedWhenRecovered; }
    public void setFirearmLoadedWhenRecovered(boolean b) { this.firearmLoadedWhenRecovered = b; }

    // ── Jewelry ────────────────────────────────────────────────────────────────
    public String getJewelryType() { return jewelryType; }
    public void setJewelryType(String s) { this.jewelryType = s; }
    public String getJewelryMaterial() { return jewelryMaterial; }
    public void setJewelryMaterial(String s) { this.jewelryMaterial = s; }
    public String getJewelryEstimatedValue() { return jewelryEstimatedValue; }
    public void setJewelryEstimatedValue(String s) { this.jewelryEstimatedValue = s; }
    public String getJewelryEngravingOrId() { return jewelryEngravingOrId; }
    public void setJewelryEngravingOrId(String s) { this.jewelryEngravingOrId = s; }

    // ── Narcotic Equipment ─────────────────────────────────────────────────────
    public String getNarcEquipType() { return narcEquipType; }
    public void setNarcEquipType(String s) { this.narcEquipType = s; }
    public String getNarcEquipDescription() { return narcEquipDescription; }
    public void setNarcEquipDescription(String s) { this.narcEquipDescription = s; }
    public String getNarcEquipSuspectedResidue() { return narcEquipSuspectedResidue; }
    public void setNarcEquipSuspectedResidue(String s) { this.narcEquipSuspectedResidue = s; }
    public boolean isNarcEquipFieldTestKitUsed() { return narcEquipFieldTestKitUsed; }
    public void setNarcEquipFieldTestKitUsed(boolean b) { this.narcEquipFieldTestKitUsed = b; }

    // ── Narcotics ──────────────────────────────────────────────────────────────
    public String getNarcDrugType() { return narcDrugType; }
    public void setNarcDrugType(String s) { this.narcDrugType = s; }
    public String getNarcNetWeight() { return narcNetWeight; }
    public void setNarcNetWeight(String s) { this.narcNetWeight = s; }
    public String getNarcForm() { return narcForm; }
    public void setNarcForm(String s) { this.narcForm = s; }
    public String getNarcPackaging() { return narcPackaging; }
    public void setNarcPackaging(String s) { this.narcPackaging = s; }
    public boolean isNarcFieldTestPerformed() { return narcFieldTestPerformed; }
    public void setNarcFieldTestPerformed(boolean b) { this.narcFieldTestPerformed = b; }
    public String getNarcFieldTestResult() { return narcFieldTestResult; }
    public void setNarcFieldTestResult(String s) { this.narcFieldTestResult = s; }

    // ── Vehicle ────────────────────────────────────────────────────────────────
    public String getVehicleBodyType() { return vehicleBodyType; }
    public void setVehicleBodyType(String s) { this.vehicleBodyType = s; }
    public String getVehicleMake() { return vehicleMake; }
    public void setVehicleMake(String s) { this.vehicleMake = s; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String s) { this.vehicleModel = s; }
    public String getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(String s) { this.vehicleYear = s; }
    public String getVehicleColor() { return vehicleColor; }
    public void setVehicleColor(String s) { this.vehicleColor = s; }
    public String getVehicleVin() { return vehicleVin; }
    public void setVehicleVin(String s) { this.vehicleVin = s; }
    public String getVehicleLicensePlate() { return vehicleLicensePlate; }
    public void setVehicleLicensePlate(String s) { this.vehicleLicensePlate = s; }
    public String getVehicleLicenseState() { return vehicleLicenseState; }
    public void setVehicleLicenseState(String s) { this.vehicleLicenseState = s; }
    public boolean isVehicleReportedStolen() { return vehicleReportedStolen; }
    public void setVehicleReportedStolen(boolean b) { this.vehicleReportedStolen = b; }
    public boolean isVehicleImpounded() { return vehicleImpounded; }
    public void setVehicleImpounded(boolean b) { this.vehicleImpounded = b; }

    // ── Weapon ─────────────────────────────────────────────────────────────────
    public String getWeaponType() { return weaponType; }
    public void setWeaponType(String s) { this.weaponType = s; }
    public String getWeaponMake() { return weaponMake; }
    public void setWeaponMake(String s) { this.weaponMake = s; }
    public String getWeaponModel() { return weaponModel; }
    public void setWeaponModel(String s) { this.weaponModel = s; }
    public String getWeaponSerialNumber() { return weaponSerialNumber; }
    public void setWeaponSerialNumber(String s) { this.weaponSerialNumber = s; }
    public String getWeaponLength() { return weaponLength; }
    public void setWeaponLength(String s) { this.weaponLength = s; }
    public boolean isWeaponReportedStolen() { return weaponReportedStolen; }
    public void setWeaponReportedStolen(boolean b) { this.weaponReportedStolen = b; }
}
