/* Model-specific overview data, cross-checked against the cited manufacturer and specification sources. */
window.ZINE_DEVICE_OVERVIEW_GROUPS = [
  {
    key: "display",
    label: "DISPLAY",
    icon: '<svg class="device-overview-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><rect x="6" y="2.5" width="12" height="19" rx="2"></rect><path d="M10 18.5h4"></path></svg>'
  },
  {
    key: "platform",
    label: "PLATFORM & MEMORY",
    icon: '<svg class="device-overview-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><rect x="7" y="7" width="10" height="10" rx="1.5"></rect><path d="M9.5 2.5v3M14.5 2.5v3M9.5 18.5v3M14.5 18.5v3M2.5 9.5h3M2.5 14.5h3M18.5 9.5h3M18.5 14.5h3"></path></svg>'
  },
  {
    key: "camera",
    label: "CAMERA",
    icon: '<svg class="device-overview-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M4 7h3l1.5-2h7L17 7h3a1.5 1.5 0 0 1 1.5 1.5v10A1.5 1.5 0 0 1 20 20H4a1.5 1.5 0 0 1-1.5-1.5v-10A1.5 1.5 0 0 1 4 7Z"></path><circle cx="12" cy="13" r="3.5"></circle></svg>'
  },
  {
    key: "battery",
    label: "BATTERY",
    icon: '<svg class="device-overview-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><rect x="3.5" y="7" width="17" height="10" rx="2"></rect><path d="M22 10v4M8 12h8"></path></svg>'
  }
];

window.ZINE_DEVICE_OVERVIEWS = {
  "a15-4g": {
    display: { primary: '6.5"', secondary: "1080 × 2340 pixels", refreshRate: "90Hz" },
    camera: { primary: "50MP", secondary: "1080p @ 30fps" },
    platform: { primary: "MediaTek Helio G99", secondary: "4/6/8GB RAM" },
    battery: { primary: "5,000mAh", secondary: "25W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_a15-12637.php",
      "https://www.samsung.com/uk/business/smartphones/galaxy-a/galaxy-a15-blue-black-128gb-sm-a155fzkdeub/",
      "https://devicesupport.three.co.uk/guides/device/Samsung/GalaxyA15LTE",
      "https://www.samsung.com/ph/smartphones/galaxy-a/galaxy-a15-blue-black-256gb-sm-a155fzkiphl/"
    ],
    notes: "For SM-A155F and related 4G variants. RAM varies by country. Battery capacity is 5,000mAh typical; rated capacity is 4,900mAh."
  },
  "a15-5g": {
    display: { primary: '6.5"', secondary: "1080 × 2340 pixels", refreshRate: "90Hz" },
    camera: { primary: "50MP", secondary: "1080p @ 30fps" },
    platform: { primary: "MediaTek Dimensity 6100+", secondary: "4/6/8GB RAM" },
    battery: { primary: "5,000mAh", secondary: "25W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_a15_5g-12638.php",
      "https://www.samsung.com/uk/business/smartphones/galaxy-a/galaxy-a15-5g-blue-black-128gb-sm-a156bzkdeub/",
      "https://www.phonearena.com/phones/Samsung-Galaxy-A15-5G_id12264"
    ],
    notes: "Aligned to the international 5G model SM-A156B family; exact RAM/storage availability varies by region. Battery is 5,000mAh typical (4,900mAh rated)."
  },
  "a16-4g": {
    display: { primary: '6.7"', secondary: "1080 × 2340 pixels", refreshRate: "90Hz" },
    camera: { primary: "50MP", secondary: "1080p @ 30fps" },
    platform: { primary: "MediaTek Helio G99", secondary: "4/6/8GB RAM" },
    battery: { primary: "5,000mAh", secondary: "25W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_a16-13383.php",
      "https://www.samsung.com/levant/smartphones/galaxy-a/galaxy-a16-black-128gb-sm-a165fzkgmea/",
      "https://www.samsung.com/uk/business/smartphones/galaxy-a/galaxy-a16-black-128gb-sm-a165fzkbeub/",
      "https://www.gsmarena.com/samsung_galaxy_a16_4g-review-2770p5.php",
      "https://www.samsung.com/za/smartphones/galaxy-a/galaxy-a16-black-128gb-sm-a165fzkdafa/",
      "https://www.samsung.com/africa_en/smartphones/galaxy-a/galaxy-a16-black-256gb-sm-a165fzkiafb/"
    ],
    notes: "For the 4G SM-A165F family. Physical RAM and storage configurations vary by market. Battery is 5,000mAh typical (4,860mAh rated)."
  },
  "a16-5g": {
    display: { primary: '6.7"', secondary: "1080 × 2340 pixels", refreshRate: "90Hz" },
    camera: { primary: "50MP", secondary: "1080p @ 30fps" },
    platform: { primary: "Exynos 1330 / Dimensity 6300 (regional)", secondary: "4/6/8GB RAM" },
    battery: { primary: "5,000mAh", secondary: "25W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_a16_5g-13346.php",
      "https://www.samsung.com/ph/smartphones/galaxy-a/galaxy-a16-5g-blue-black-256gb-sm-a166pzkiphl/",
      "https://www.samsung.com/us/support/answer/ANS10004543/",
      "https://www.gsmarena.com/samsung_galaxy_a16_5g-review-2758p5.php",
      "https://www.samsung.com/ca/smartphones/galaxy-a/galaxy-a16-5g-blue-black-128gb-sm-a166wzkaxac/"
    ],
    notes: "No regional model code is set on the catalog entry. The chipset varies between Exynos 1330 and Dimensity 6300 by model/market. The selected video value is the standard rear-camera app's 1080p@30fps mode; Samsung documentation differs by region. Battery is 5,000mAh typical (4,860mAh rated)."
  },
  "a25-5g": {
    display: { primary: '6.5"', secondary: "1080 × 2340 pixels", refreshRate: "120Hz" },
    camera: { primary: "50MP", secondary: "4K @ 30fps" },
    platform: { primary: "Samsung Exynos 1280", secondary: "4/6/8GB RAM" },
    battery: { primary: "5,000mAh", secondary: "25W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_a25-12555.php",
      "https://www.samsung.com/ae/business/smartphones/galaxy-a/galaxy-a25-5g-yellow-256gb-sm-a256ezyhmea/",
      "https://image-us.samsung.com/SamsungUS/samsungbusiness/mobile/phones/galaxy-a/HHP449741_A25_5G_SpecSheet.pdf",
      "https://www.samsung.com/uk/business/smartphones/galaxy-a/galaxy-a25-5g-yellow-128gb-sm-a256bzydeub/"
    ],
    notes: "50MP primary camera includes OIS. RAM options vary across markets; Samsung's cited SM-A256E listing offers 6GB/8GB, while GSMArena lists additional 4GB configurations. Battery is 5,000mAh typical (4,905mAh rated)."
  },
  "a34-5g": {
    display: { primary: '6.6"', secondary: "1080 × 2340 pixels", refreshRate: "120Hz" },
    camera: { primary: "48MP", secondary: "4K @ 30fps" },
    platform: { primary: "MediaTek Dimensity 1080", secondary: "4/6/8GB RAM" },
    battery: { primary: "5,000mAh", secondary: "25W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_a34-12074.php",
      "https://www.samsung.com/hk_en/business/smartphones/galaxy-a/galaxy-a34-5g-awesome-silver-sm-a3460zsctgy/",
      "https://deviceguides.ee.co.uk/samsung/galaxy-a34-5g-android-13/specifications/",
      "https://www.samsung.com/ph/smartphones/galaxy-a/galaxy-a34-5g-awesome-graphite-128gb-sm-a346ezkcphl/"
    ],
    notes: "48MP primary/wide camera. GSMArena lists 4/6/8GB configurations; Samsung's cited regional listing advertises 6GB/8GB. Battery is 5,000mAh typical (4,905mAh rated)."
  },
  "a53-5g": {
    display: { primary: '6.5"', secondary: "1080 × 2400 pixels", refreshRate: "120Hz" },
    camera: { primary: "64MP", secondary: "4K @ 30fps" },
    platform: { primary: "Samsung Exynos 1280", secondary: "4/6/8GB RAM" },
    battery: { primary: "5,000mAh", secondary: "25W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_a53_5g-11268.php",
      "https://news.samsung.com/global/galaxy-a53-5g-and-galaxy-a33-5g-awesome-mobile-experiences-open-to-everyone",
      "https://news.samsung.com/in/samsung-launches-galaxy-a53-5g-with-64mp-ois-camera-and-5nm-processor-announces-exciting-pre-book-offers",
      "https://www.samsung.com/ca/business/smartphones/galaxy-a/galaxy-a53-5g-sm-a536wzkaxac/",
      "https://www.samsung.com/ie/business/smartphones/galaxy-a/galaxy-a53-5g-128gb-awesome-blue-sm-a536blbneub/"
    ],
    notes: "64MP primary camera includes OIS. Samsung's official announcement advertises 6GB/8GB RAM; GSMArena also lists a 4GB option across regional configurations. Battery is 5,000mAh typical (4,860mAh rated/minimum)."
  },
  "a52s-5g": {
    display: { primary: '6.5"', secondary: "1080 × 2400 pixels", refreshRate: "120Hz" },
    camera: { primary: "64MP", secondary: "4K @ 30fps" },
    platform: { primary: "Qualcomm Snapdragon 778G 5G", secondary: "4/6/8GB RAM" },
    battery: { primary: "4,500mAh", secondary: "25W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_a52s_5g-11039.php",
      "https://www.samsung.com/ph/business/smartphones/galaxy-a/galaxy-a52s-5g-awesome-white-256gb-sm-a528bzwiphl/",
      "https://news.samsung.com/uk/the-samsung-galaxy-a52s-5g-provides-enriched-entertainment-at-excellent-value",
      "https://www.samsung.com/ie/business/smartphones/galaxy-a/galaxy-a52s-5g-awesome-mint-128gb-sm-a528blgceua/"
    ],
    notes: "64MP wide/main camera includes OIS. RAM availability varies by market. Battery is 4,500mAh typical (4,370mAh rated/minimum); a 25W charger may be sold separately."
  },
  "a11-4g": {
    display: { primary: '6.4"', secondary: "720 × 1560 pixels" },
    camera: { primary: "13MP", secondary: "1080p @ 30fps" },
    platform: { primary: "Qualcomm Snapdragon 450", secondary: "2/3GB RAM" },
    battery: { primary: "4,000mAh", secondary: "15W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_a11-10132.php",
      "https://www.phonemore.com/specs/samsung/galaxy-a11/sm-a115f/",
      "https://www.samsung.com/ph/support/model/SM-A115FZBGXTC/",
      "https://image-us.samsung.com/SamsungUS/samsungbusiness/products/mobile/phones/galaxy-a/galaxy-a11/sm-a115uzkaxaa/Galaxy_A11_Series_Datasheet_Unlocked.pdf"
    ],
    notes: "Display refresh rate is not specified in the model-matched Samsung and GSMArena sources. RAM is listed for the exact SM-A115F; other regional Galaxy A11 models may offer up to 4GB. Rear-camera maximum is 1080p@30fps. The Samsung datasheet is for SM-A115U and was not used to transfer its different video-frame-rate claim to SM-A115F."
  },
  "a24-4g": {
    display: { primary: '6.5"', secondary: "1080 × 2340 pixels", refreshRate: "90Hz" },
    camera: { primary: "50MP", secondary: "1080p @ 30fps" },
    platform: { primary: "MediaTek Helio G99", secondary: "4/6/8GB RAM" },
    battery: { primary: "5,000mAh", secondary: "25W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_a24_4g-12176.php",
      "https://news.samsung.com/my/samsung-malaysia-redefines-awesome-with-new-and-improved-galaxy-a24",
      "https://www.samsung.com/ae/smartphones/galaxy-a/galaxy-a24-silver-128gb-sm-a245fzsumea/"
    ],
    notes: "For SM-A245F. RAM availability varies by market; RAM Plus is virtual memory and is not included in the physical RAM figure. Battery is 5,000mAh typical (4,860mAh rated)."
  },
  "tab-a9-plus-5g-x216b": {
    display: { primary: '11.0"', secondary: "1920 × 1200 pixels (WUXGA)", refreshRate: "90Hz" },
    camera: { primary: "8MP", secondary: "1080p @ 30fps" },
    platform: { primary: "Qualcomm Snapdragon 695 5G", secondary: "4/8GB RAM" },
    battery: { primary: "7,040mAh", secondary: "15W wired" },
    sources: [
      "https://www.gsmarena.com/samsung_galaxy_tab_a+-12617.php",
      "https://www.samsung.com/levant/tablets/galaxy-tab-a/galaxy-tab-a9-plus-5g-graphite-128gb-sm-x216bzaemea/",
      "https://news.samsung.com/in/samsung-galaxy-tab-a9-and-galaxy-tab-a9-entertainment-and-productivity-engineered-for-everyone",
      "https://me.pcmag.com/en/tablets/22817/samsung-galaxy-tab-a9",
      "https://www.samsung.com/sg/tablets/galaxy-tab-a/galaxy-tab-a9-plus-5g-mystic-navy-64gb-sm-x216bdbaxsp/"
    ],
    notes: "For SM-X216B 5G. RAM/storage configurations vary by market. Samsung lists a 7,040mAh typical battery and 15W charging; adapter may be sold separately."
  }
};
