/* eslint-disable sort-keys-fix/sort-keys-fix */
/** What the history texts are built from (see HistoryEvent in api/types.ts). */
type HistoryEventTexts = {
  description: string
  key?: string | null
  licence?: string | null
  linkedTo?: string | null
  photoNumber?: number | null
  type?: string
}

/** English texts. `fr.ts` must have the same keys: TypeScript checks it. */
export const en = {
  language: {
    switchLabel: 'Afficher le site en français',
    switchText: 'FR'
  },
  layout: {
    navLabel: 'Main',
    search: 'Search',
    api: 'API',
    contribute: 'Contribute',
    licences: 'Licences',
    footerCode: 'code',
    footerUnder: 'under',
    footerPhotos: 'photos under their own open licences',
    dataset: 'Download the dataset',
    apiKey: 'API key',
    review: 'Review'
  },
  common: {
    loading: 'Loading…',
    sending: 'Sending…',
    cancel: 'Cancel',
    optional: '(optional)',
    unnamedVessel: 'Unnamed vessel',
    photos: (count: number) => `${count} photo${count === 1 ? '' : 's'}`,
    noPhotoYet: 'No photo yet',
    copied: 'Copied',
    reject: 'Reject'
  },
  errors: {
    /** No translation: the server's message is shown, it is in English and more precise */
    byCode: {} as Partial<Record<string, string>>,
    generic: 'Something went wrong. Please try again.'
  },
  home: {
    pageTitle: 'Open Vessel Photos — free photos of any vessel, by CFR or MMSI',
    searchPageTitle: (query: string) => `Search: ${query}`,
    chips: ['Free', 'Open-source code', 'Openly licensed photos'],
    title: 'The open library of vessel photos.',
    lead: 'Search by name, CFR or MMSI. Every photo is openly licensed and free to reuse, with its credit.',
    searchFormLabel: 'Search vessels',
    searchLabel: 'Search by name, CFR or MMSI',
    searchPlaceholder: 'Name, CFR (FRA000123456) or MMSI (227000101)',
    searchPlaceholderShort: 'Name, CFR or MMSI',
    searchButton: 'Search',
    try: 'Try',
    resultsLabel: 'Results',
    resultsFor: (query: string) => `Results for “${query}”`,
    recentlyAdded: 'Recently added',
    searchUnavailable: 'The search is unavailable right now. Please try again later.',
    noMatch: (query: string) => `No vessel matches “${query}”.`,
    noPhotoOfKey: (key: string) => `No photo of ${key} yet.`,
    contributeFirst: 'Contribute the first one →',
    searchByKey: 'Search by CFR or MMSI to be sure, or',
    contributeAPhoto: 'contribute a photo',
    noPhotoAtAll: 'No photo yet.',
    apiTitle: 'One URL per vessel.',
    apiLead: 'No key, no account. Drop the image straight into your app, or fetch the JSON for credit and licence.',
    apiButton: 'Read the API docs',
    contributeTitle: 'Contribute a photo',
    contributeText: 'Upload your own picture, pick an open licence, and it goes live once reviewed.',
    contributeLink: 'Upload a photo →',
    openTitle: 'Open for good',
    openText: 'Open-source code and a full export of the data: anyone can keep the library going.',
    openLink: 'See the licences →',
    datasetTitle: 'Download everything',
    datasetText: 'Full dataset export of vessels, photo URLs, credits and licences, always up to date.',
    datasetLink: 'Open data (JSON) →'
  },
  chart: {
    commons: 'Commons',
    creditsIntro: 'Photos on the chart:',
    creditsBy: 'by',
    creditsOutro: ', cropped, from Wikimedia Commons.'
  },
  vesselCard: {
    photoAlt: (name: string, credit: string) => `${name}, by ${credit}`
  },
  vessel: {
    source: 'Source',
    pageTitle: (name: string, keys: string) => `${name} (${keys}) — vessel photos`,
    notFoundPageTitle: (key: string) => `No vessel with ${key}`,
    notFoundTitle: (key: string) => `No vessel with ${key} yet`,
    unavailable: 'Unavailable',
    notFoundText: 'Nobody has shared a photo of it so far.',
    contributeFirst: 'Contribute the first photo →',
    invalidKey: (keyType: string) => `This doesn't look like a valid ${keyType}.`,
    backToSearch: 'Back to search',
    tryLater: 'Please try again later.',
    breadcrumb: 'Breadcrumb',
    search: 'Search',
    photoAlt: (name: string, credit: string) => `${name}, by ${credit}`,
    photoOf: (n: number, count: number) => `Photo ${n} of ${count}`,
    taken: (date: string) => `taken ${date}`,
    noPhotoOf: (name: string) => `No photo of ${name} yet.`,
    addFirstPhoto: 'Add the first photo',
    photosLabel: 'Photos of this vessel',
    showPhoto: (n: number) => `Show photo ${n}`,
    addPhoto: 'Add a photo',
    reportPhoto: (n: number) => `Report photo ${n}`,
    flag: (flag: string) => `Flag · ${flag} (from CFR)`,
    useThisPhoto: 'Use this photo',
    creditLine: 'Credit line — required by the licence',
    creditLineOptional: ' (optional for this licence, but appreciated)',
    imageSize: 'Image size',
    sizeFull: 'Full',
    imageUrl: 'Image URL, either key works',
    copyCreditAndUrl: 'Copy credit + URL',
    json: 'JSON for this vessel',
    history: 'History',
    historyDate: 'Date',
    historyChange: 'Change',
    historyBy: 'By',
    historyEvent: (event: HistoryEventTexts): string => {
      switch (event.type) {
        case 'VESSEL_ADDED':
          return 'Vessel added'
        case 'PHOTO_PUBLISHED':
          return `Photo ${event.photoNumber} published (${event.licence})`
        case 'KEY_LINKED':
          return event.linkedTo ? `${event.key} linked to ${event.linkedTo}` : `${event.key} added`
        default:
          return event.description
      }
    }
  },
  keyLink: {
    thanks: (keyType: string) => `Thanks! The ${keyType} will be added once a reviewer has checked it.`,
    open: (keyType: string) => `Know its ${keyType}? Suggest it →`,
    yourName: 'Your name (optional)',
    submit: (keyType: string) => `Suggest this ${keyType}`
  },
  report: {
    reasons: {
      WRONG_VESSEL: 'It is not this vessel',
      COPYRIGHT: 'It is my photo / not openly licensed',
      PRIVACY: 'It shows people or private details',
      OTHER: 'Other'
    },
    thanks: (n: number) => `Thanks, a reviewer will look at photo ${n}.`,
    title: (n: number) => `Report photo ${n}`,
    reason: 'Reason',
    details: 'Details (optional)',
    submit: 'Send report'
  },
  contribute: {
    pageTitle: 'Contribute a vessel photo',
    cfrHint: '3-letter country code + 9 characters, from the EU fleet register.',
    cfrLabel: 'CFR number',
    mmsiHint: '9 digits, the radio identity shown on AIS.',
    mmsiLabel: 'MMSI number',
    licences: {
      'CC-BY-4.0': 'Anyone can reuse it with your credit.',
      'CC-BY-SA-4.0': 'Reuse with credit; changes shared alike.',
      'CC0-1.0': 'Public domain. No credit required.'
    } as Record<string, string>,
    fileTooLarge: 'This file is larger than 15 MB.',
    thanksTitle: 'Thank you!',
    thanksText: (vessel: string) =>
      `Your photo of ${vessel} is in the review queue. It goes live once a volunteer has checked it.`,
    sendAnother: 'Send another photo',
    backTo: (vessel: string) => `Back to ${vessel}`,
    theVessel: 'the vessel',
    title: 'Contribute a photo',
    lead: "Your own picture, an open licence, and the vessel's CFR or MMSI. It goes live once a volunteer has reviewed it.",
    step1: '1 · Which vessel?',
    keysHint: 'Fill in one of them, or both if you know them.',
    keysConflict: 'This CFR and this MMSI are those of two different vessels: check them.',
    matches: 'Matches',
    anUnnamedVessel: 'an unnamed vessel',
    noVesselYet: 'No vessel with this key yet — it will be created when your photo is approved.',
    vesselName: 'Vessel name',
    vesselNamePlaceholder: 'e.g. Ker Avel',
    step2: '2 · The photo',
    yourUpload: 'Your upload',
    choosePhoto: 'Choose a photo',
    fileHint: 'JPEG or PNG · the vessel clearly visible · 15 MB max',
    dateTaken: 'Date taken',
    place: 'Place',
    placePlaceholder: 'e.g. Concarneau harbour',
    step3: '3 · Credit and licence',
    creditAs: 'Credit as',
    creditPlaceholder: 'Your name or handle',
    licence: 'Licence',
    rights: (licence: string) => `I took this photo, or I hold the right to publish it under ${licence}.`,
    email: 'Email',
    emailHint: '(optional, only seen by reviewers if they have a question)',
    submit: 'Send for review',
    noAccount: 'No account needed.',
    nextTitle: 'What happens next',
    nextReceived: 'Received.',
    nextReceivedText: 'Your photo waits in the review queue.',
    nextReviewed: 'Reviewed.',
    nextReviewedText: 'A volunteer checks the vessel matches its key and the licence is clear.',
    nextPublished: 'Published.',
    nextPublishedText: 'Live on the site and the API under both CFR and MMSI.',
    tipsTitle: 'A good vessel photo',
    tips: [
      'The whole hull in frame, side-on if you can',
      'Name or registration readable',
      "No people's faces up close",
      'Only photos you took yourself'
    ],
    exif: 'Location data (EXIF/GPS) is removed from every photo.',
    scripts: 'Prefer scripts?',
    scriptsEnd: 'does the same.'
  },
  apiKey: {
    pageTitle: 'Request an API key',
    title: 'Request an API key',
    lead: 'Reading the API is free and open. An API key is only needed to send data with a script: contribute photos, suggest keys or report photos through POST requests.',
    received: 'Request received.',
    receivedText: (email: string) => `A reviewer will send your API key to ${email} once your request is approved.`,
    name: 'Your name',
    email: 'Email',
    emailHint: 'The key is sent to this address.',
    organisation: 'Organisation or project',
    use: 'What will you use it for?',
    usePlaceholder: 'e.g. Import the photos taken by our harbour team, about 50 a month.',
    terms: 'I will only send photos their authors agreed to publish under an open licence, with their credit.',
    submit: 'Request a key',
    howTitle: 'How to use it',
    howHeader: 'Send the key in the x-api-key header of your POST requests:',
    howSecret:
      'Keep it secret: never put it in a web page or a mobile app. Everything sent is still reviewed before publication. See the',
    apiDocs: 'API docs'
  },
  docs: {
    pageTitle: 'Free vessel photo API, by CFR or MMSI',
    chip: 'API v1 · no key required',
    title: 'The vessel photo API',
    lead: 'CFR and MMSI are both first-class keys: every URL exists under each of them and returns the same vessel and photos. A vessel can have one key or both — a non-EU ship with only an MMSI works just as well.',
    keyCfr: 'Key · CFR (EU fleet register)',
    keyMmsi: 'Key · MMSI (radio identity)',
    endpoints: 'Endpoints',
    endpointDescriptions: [
      'Vessel, both of its keys, its registration and port, and all its published photos with credit and licence.',
      'The main photo as an image, ready for an <img> tag. ?size=320|800|full (default 800).',
      'A specific photo; the same n under either key is the same file. Same ?size parameter.',
      'Search by name, port, registration, or the beginning of a CFR or MMSI. Up to 24 results.',
      'Vessels with the most recently published photos. ?limit=12 (max 48).',
      'The whole dataset: every vessel with its photo URLs, credits and licences.',
      'Contribute a photo (multipart: file, credit, licence, takenOn?, place?, email?, vesselName?, plus mmsi? under a CFR or cfr? under an MMSI), under whichever key you know. Published after review. Needs an API key.',
      'Suggest the missing key, e.g. {"keyType": "cfr", "key": "FRA000123456"}. Applied after review. Needs an API key.',
      'Report a photo: {"reason": "WRONG_VESSEL|COPYRIGHT|PRIVACY|OTHER", "message": "…"}. Needs an API key.',
      'Request an API key: {"name", "email", "organisation"?, "intendedUse"}. Sent by email once approved.'
    ],
    example: 'Example',
    errorsIntro: 'Errors come back as',
    errorsOutro: 'with a 400, 404 or 409 status.',
    apiKeyTitle: 'API key',
    apiKeyText1: 'Every GET is open, without key. POST requests need an API key in the x-api-key header:',
    apiKeyLink: 'request one',
    apiKeyText2: ', it is free. Without it, they return a 401.',
    rateLimitsTitle: 'Rate limits',
    rateLimitsText:
      'Up to 600 GET per minute per IP, and 1,000 POST per hour per API key. Every response tells where you stand in X-RateLimit-Limit, X-RateLimit-Remaining and X-RateLimit-Reset (seconds). Above the limit, requests return a 429 with a Retry-After header. Need more? Download the whole dataset from /v1/export.json, or ask for a higher limit.',
    chips: ['CORS enabled', 'Reading needs no API key', 'Always show the credit'],
    licencesTitle: 'Licences',
    licencesText:
      'Every photo is published by its author under one of these licences. Reuse it freely, as long as you show the credit line given in attribution when the licence asks for it.',
    licenceBy: '— reuse with credit.',
    licenceBySa: '— reuse with credit; changes shared under the same licence.',
    licenceCc0: '— public domain, no credit required.',
    licenceOlder:
      'Photos imported from Wikimedia Commons or Flickr keep their own licence, which can also be an older version of these (CC BY or CC BY-SA 1.0 to 3.0) or the public domain.',
    selfHostTitle: 'Self-host it',
    selfHostText:
      'The service is one container (Spring Boot serving the API and this web app) and one PostgreSQL database, which also stores the images. Set OPENVESSELPHOTOS_DATABASE_URL and OPENVESSELPHOTOS_ADMIN_PASSWORD, and run it.'
  },
  notFound: {
    title: 'Page not found',
    text: "This page doesn't exist.",
    link: 'Search vessel photos'
  },
  admin: {
    pageTitle: 'Review',
    queueTitle: 'Review queue',
    statistics: 'Statistics',
    signOut: 'Sign out',
    loginTitle: 'Review',
    loginText: 'Sign in with the admin password to review contributions.',
    password: 'Admin password',
    signIn: 'Sign in',
    queueLabel: 'Queue',
    tabs: { photos: 'Photos', keyLinks: 'Key links', reports: 'Reports', apiKeys: 'API keys' },
    rejectReasons: [
      "Vessel doesn't match the key",
      "Not the contributor's own photo",
      'Poor quality or vessel not visible',
      'Duplicate of an existing photo',
      'Other'
    ],
    reportReasons: {
      COPYRIGHT: 'Copyright / licence',
      OTHER: 'Other',
      PRIVACY: 'People or private details',
      WRONG_VESSEL: 'Wrong vessel'
    } as Record<string, string>,
    requested: (date: string) => `Requested ${date}`,
    organisation: 'Organisation or project',
    approveKey: 'Approve and create the key',
    apiKeyRequest: 'API key request',
    knownKey: 'Known key',
    suggestedKey: 'Suggested key',
    suggestedBy: (date: string, by: string) => `Suggested ${date} by ${by}`,
    anonymous: 'anonymous',
    linkChecks: ['Both keys found in a public register', 'Same vessel name and flag'],
    linkKeys: 'Link the keys',
    addKey: (key: string) => `Add ${key}`,
    newPhoto: 'New photo',
    firstPhoto: 'First photo · creates vessel',
    reported: (date: string, n: number | null, credit: string, licence: string) =>
      `Reported ${date} · photo ${n} by ${credit} (${licence})`,
    reason: 'Reason',
    removePhoto: 'Remove the photo',
    dismissReport: 'Dismiss the report',
    reportSubtitle: (reason: string, n: number | null) => `Report · ${reason} · photo ${n}`,
    nothingToReview: 'Nothing to review here.',
    pendingItems: 'Pending items',
    newVessel: 'New vessel',
    submitted: (date: string, by: string) => `Submitted ${date} by ${by}`,
    sentWithKey: (name: string) => ` · sent with the API key of ${name}`,
    submittedKey: 'Submitted key',
    licence: 'Licence',
    taken: 'Taken',
    size: 'Size',
    vesselName: 'Vessel name',
    photoChecks: [
      'Vessel in the photo matches the key',
      'Contributor plausibly took it themselves',
      'No close-up faces or private details',
      'Not a duplicate'
    ],
    checklist: 'Checklist',
    rejectWhy: 'If rejecting, why?',
    approvePhoto: 'Approve and publish',
    previewAlt: 'Submission preview',
    keyCreatedFor: (name: string) => `API key created for ${name}`,
    sendItNow: "Send it now: it won't be shown again.",
    emailIt: (email: string) => `Email it to ${email}`,
    copyKey: 'Copy the key',
    done: 'Done',
    emailSubject: 'Your Open Vessel Photos API key',
    emailBody: (name: string, key: string, docsUrl: string) =>
      `Hello ${name},\n\nYour API key request was approved. Here is your key:\n\n${key}\n\nSend it in the x-api-key header of your POST requests, and keep it secret. Documentation: ${docsUrl}\n`,
    activeKeys: 'Active API keys',
    noActiveKey: 'No active key.',
    keyColumns: {
      name: 'Name',
      email: 'Email',
      key: 'Key',
      created: 'Created',
      lastUsed: 'Last used',
      actions: 'Actions'
    },
    revoke: 'Revoke',
    importTitle: 'Import vessels',
    importText1: 'Creates vessels without photos, so that their pages exist and can be found. Accepts an export of the',
    importRegister: 'EU fleet register',
    importText2:
      '(CSV, latest state of the vessels of a country: those retired, destroyed or exported are left out), or a CSV with the columns cfr, mmsi, name, registration, port. Existing vessels are never overwritten: only their missing data is filled in. 15 MB max.',
    csvFile: 'CSV file',
    importing: 'Importing…',
    import: 'Import',
    photoImportTitle: 'Import photos',
    photoImportText:
      'Publishes openly licensed photos without review, e.g. from Wikimedia Commons. CSV columns: mmsi and/or cfr, vessel_name, author, licence, file_page (page of the original photo), image_1600_url and, optionally, date and note (shown with the photo). Images are downloaded from Wikimedia Commons or Flickr one at a time, in the background: this can take a few hours. Photos already imported are skipped, so the same file can be sent again after an interruption.',
    photoImportRunning: 'Import running…',
    photoImportFinished: 'Last import finished.',
    photoImportProgress: (s: {
      done: number
      total: number
      imported: number
      alreadyImported: number
      failed: number
      invalid: number
    }) =>
      `${s.done} / ${s.total} photos: ${s.imported} imported, ${s.alreadyImported} already there, ${s.failed} failed. ${s.invalid} invalid lines skipped.`,
    stopImport: 'Stop',
    photoNotesTitle: 'Photo notes',
    photoNotesText:
      'Sets the notes shown with published photos, e.g. "Photographed under its former name LE CRI DES FLOTS". CSV columns: cfr (or mmsi), n (the photo number) and note. An empty note removes it.',
    setNotes: 'Set the notes',
    photoNotesResult: (r: { read: number; updated: number; unchanged: number; errors: string[] }) =>
      `${r.read} lines read: ${r.updated} notes set, ${r.unchanged} already up to date, ${r.errors.length} errors.`,
    importResult: (r: {
      read: number
      created: number
      updated: number
      unchanged: number
      skipped: number
      outOfFleet: number
    }) =>
      `${r.read} lines read: ${r.created} vessels created, ${r.updated} completed, ${r.unchanged} already up to date, ${r.outOfFleet} no longer in the fleet, ${r.skipped} skipped.`
  },
  stats: {
    pageTitle: 'Statistics',
    title: 'Statistics',
    backToQueue: '← Review queue',
    period: 'Period',
    lastDays: (days: number) => (days === 365 ? 'Last year' : `Last ${days} days`),
    imageViews: 'Photo views',
    imageViewsElsewhere: (count: string, sites: number) => `${count} on ${sites} other site${sites > 1 ? 's' : ''}`,
    apiCalls: 'API calls',
    apiCallsWithKey: (count: string) => `${count} with an API key`,
    activeKeys: 'Active API keys',
    activeKeysHint: 'Used during the period / all active keys',
    photos: 'Published photos',
    vessels: (count: string) => `${count} vessels`,
    imageViewsPerDay: 'Photo views per day',
    apiCallsPerDay: 'API calls per day',
    vesselsAddedPerDay: (total: string) => `Vessels added per day (${total} in all, imports included)`,
    imageSources: 'Sites showing the photos',
    apiSources: 'API calls by site',
    apiKeys: 'API calls by key',
    noKeyCalls: 'No call with an API key yet.',
    noData: 'Nothing counted yet.',
    site: 'Site',
    count: 'Count',
    key: 'Key',
    calls: 'Calls',
    lastUsed: 'Last used',
    website: 'This website',
    direct: 'Apps and scripts (no referrer)',
    other: 'Other sites',
    note: 'Counted without personal data: images by the site that shows them (Referer header), API calls by key. A browser keeps a photo for a day, so it is counted once a day per visitor at most; the last minute is not counted yet.'
  }
}

export type Messages = typeof en
