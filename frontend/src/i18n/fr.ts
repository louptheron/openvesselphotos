/* eslint-disable sort-keys-fix/sort-keys-fix */
import type { Messages } from './en'

/** Non-breaking space, before « : ; ? ! » and inside « » in French */
const _ = ' '

export const fr: Messages = {
  language: {
    switchLabel: 'Show the website in English',
    switchText: 'EN'
  },
  layout: {
    navLabel: 'Principale',
    search: 'Recherche',
    api: 'API',
    contribute: 'Contribuer',
    licences: 'Licences',
    footerCode: 'code',
    footerUnder: 'sous',
    footerPhotos: 'photos sous leurs propres licences libres',
    dataset: 'Télécharger les données',
    apiKey: 'Clé d’API',
    review: 'Modération'
  },
  common: {
    loading: 'Chargement…',
    sending: 'Envoi…',
    cancel: 'Annuler',
    optional: '(facultatif)',
    unnamedVessel: 'Navire sans nom',
    photos: (count: number) => `${count}${_}photo${count > 1 ? 's' : ''}`,
    noPhotoYet: 'Pas encore de photo',
    copied: 'Copié',
    reject: 'Refuser'
  },
  errors: {
    byCode: {
      CONFLICT: `Cette demande entre en conflit avec une donnée existante${_}: elle a déjà été traitée, ou la clé appartient à un autre navire.`,
      INVALID_IMAGE: `Image refusée${_}: elle doit être un JPEG ou un PNG d’au moins 320${_}×${_}200 pixels, de 15${_}Mo au plus.`,
      INVALID_INPUT: 'Certaines informations ne sont pas valides. Vérifiez le formulaire.',
      INVALID_KEY: `Ce CFR ou ce MMSI n’est pas valide (CFR${_}: 3 lettres et 9 caractères, MMSI${_}: 9 chiffres).`,
      NOT_FOUND: 'Introuvable.',
      TOO_MANY_REQUESTS: 'Trop de demandes. Réessayez dans quelques minutes.',
      UNAUTHORIZED: 'Mot de passe incorrect, ou session expirée.'
    },
    generic: 'Une erreur est survenue. Veuillez réessayer.'
  },
  home: {
    pageTitle: 'Open Vessel Photos — photos libres de n’importe quel navire, par CFR ou MMSI',
    searchPageTitle: (query: string) => `Recherche${_}: ${query}`,
    chips: ['Gratuit pour tous', 'Code open source', 'Photos sous licence libre'],
    title: 'La photothèque libre des navires.',
    lead: 'Cherchez par nom, CFR ou MMSI. Chaque photo est sous licence libre et réutilisable, avec son crédit.',
    searchFormLabel: 'Rechercher un navire',
    searchLabel: 'Rechercher par nom, CFR ou MMSI',
    searchPlaceholder: 'Nom, CFR (FRA000123456) ou MMSI (227000101)',
    searchPlaceholderShort: 'Nom, CFR ou MMSI',
    searchButton: 'Rechercher',
    try: 'Essayez',
    resultsLabel: 'Résultats',
    resultsFor: (query: string) => `Résultats pour «${_}${query}${_}»`,
    recentlyAdded: 'Ajoutés récemment',
    searchUnavailable: 'La recherche est indisponible pour le moment. Veuillez réessayer plus tard.',
    noMatch: (query: string) => `Aucun navire ne correspond à «${_}${query}${_}».`,
    noPhotoOfKey: (key: string) => `Pas encore de photo du ${key}.`,
    contributeFirst: 'Envoyez la première →',
    searchByKey: 'Cherchez par CFR ou MMSI pour être sûr, ou',
    contributeAPhoto: 'envoyez une photo',
    noPhotoAtAll: 'Pas encore de photo.',
    apiTitle: 'Une URL par navire.',
    apiLead:
      'Sans clé ni compte. Affichez l’image directement dans votre application, ou récupérez le JSON pour le crédit et la licence.',
    apiButton: 'Lire la documentation de l’API',
    contributeTitle: 'Envoyer une photo',
    contributeText: 'Envoyez votre propre photo, choisissez une licence libre, et elle est publiée après vérification.',
    contributeLink: 'Envoyer une photo →',
    openTitle: 'Libre pour de bon',
    openText: `Code open source et export complet des données${_}: n’importe qui peut faire vivre la photothèque.`,
    openLink: 'Voir les licences →',
    datasetTitle: 'Tout télécharger',
    datasetText: 'Export complet des navires, des URL des photos, des crédits et des licences, toujours à jour.',
    datasetLink: 'Données ouvertes (JSON) →'
  },
  coverage: {
    barLabel: 'Navires du registre de la flotte de l’UE ayant une photo',
    register: (photographed, total, count) =>
      count > 1
        ? `${photographed}${_}navires sur ${total} du registre de la flotte de l’UE ont une photo.`
        : `${photographed}${_}navire sur ${total} du registre de la flotte de l’UE a une photo.`,
    library: (photos, vessels) => `${photos}${_}photos de ${vessels}${_}navires en tout.`,
    help: 'Aidez à photographier les autres →'
  },
  chart: {
    commons: 'Commons',
    creditsIntro: `Photos sur la carte${_}:`,
    creditsBy: 'par',
    creditsOutro: ', recadrées, depuis Wikimedia Commons.'
  },
  vesselCard: {
    photoAlt: (name: string, credit: string) => `${name}, par ${credit}`
  },
  vessel: {
    source: 'Source',
    pageTitle: (name: string, keys: string) => `${name} (${keys}) — photos du navire`,
    notFoundPageTitle: (key: string) => `Aucun navire avec le ${key}`,
    notFoundTitle: (key: string) => `Aucun navire avec le ${key} pour l’instant`,
    unavailable: 'Indisponible',
    notFoundText: 'Personne n’en a encore partagé de photo.',
    contributeFirst: 'Envoyez la première photo →',
    invalidKey: (keyType: string) => `Ce ${keyType} ne semble pas valide.`,
    backToSearch: 'Retour à la recherche',
    tryLater: 'Veuillez réessayer plus tard.',
    breadcrumb: 'Fil d’Ariane',
    search: 'Recherche',
    photoAlt: (name: string, credit: string) => `${name}, par ${credit}`,
    photoOf: (n: number, count: number) => `Photo ${n} sur ${count}`,
    taken: (date: string) => `prise le ${date}`,
    noPhotoOf: (name: string) => `Pas encore de photo de ${name}.`,
    addFirstPhoto: 'Envoyer la première photo',
    photosLabel: 'Photos de ce navire',
    showPhoto: (n: number) => `Afficher la photo ${n}`,
    addPhoto: 'Ajouter une photo',
    reportPhoto: (n: number) => `Signaler la photo ${n}`,
    flag: (flag: string) => `Pavillon · ${flag} (d’après le CFR)`,
    useThisPhoto: 'Utiliser cette photo',
    creditLine: 'Ligne de crédit — exigée par la licence',
    creditLineOptional: `${_}(facultative pour cette licence, mais appréciée)`,
    imageSize: 'Taille de l’image',
    sizeFull: 'Originale',
    imageUrl: 'URL de l’image, avec l’une ou l’autre clé',
    copyCreditAndUrl: 'Copier le crédit et l’URL',
    json: 'JSON de ce navire',
    history: 'Historique',
    historyDate: 'Date',
    historyChange: 'Modification',
    historyBy: 'Par',
    historyEvent: event => {
      switch (event.type) {
        case 'VESSEL_ADDED':
          return 'Navire ajouté'
        case 'PHOTO_PUBLISHED':
          return `Photo ${event.photoNumber} publiée (${event.licence})`
        case 'KEY_LINKED':
          return event.linkedTo ? `${event.key} associé au ${event.linkedTo}` : `${event.key} ajouté`
        default:
          return event.description
      }
    }
  },
  keyLink: {
    thanks: (keyType: string) => `Merci${_}! Le ${keyType} sera ajouté après vérification.`,
    open: (keyType: string) => `Vous connaissez son ${keyType}${_}? Proposez-le →`,
    yourName: 'Votre nom (facultatif)',
    submit: (keyType: string) => `Proposer ce ${keyType}`
  },
  report: {
    reasons: {
      WRONG_VESSEL: 'Ce n’est pas ce navire',
      COPYRIGHT: 'C’est ma photo / elle n’est pas sous licence libre',
      PRIVACY: 'Elle montre des personnes ou des informations privées',
      OTHER: 'Autre'
    },
    thanks: (n: number) => `Merci, un modérateur va examiner la photo ${n}.`,
    title: (n: number) => `Signaler la photo ${n}`,
    reason: 'Motif',
    details: 'Précisions (facultatif)',
    submit: 'Envoyer le signalement'
  },
  contribute: {
    pageTitle: 'Envoyer une photo de navire',
    cfrHint: 'Code pays de 3 lettres et 9 caractères, du registre de la flotte de l’UE.',
    cfrLabel: 'Numéro CFR',
    mmsiHint: '9 chiffres, l’identité radio affichée par l’AIS.',
    mmsiLabel: 'Numéro MMSI',
    licences: {
      'CC-BY-4.0': 'Tout le monde peut la réutiliser en vous créditant.',
      'CC-BY-SA-4.0': 'Réutilisation avec crédit, modifications partagées à l’identique.',
      'CC0-1.0': 'Domaine public. Aucun crédit exigé.'
    },
    fileTooLarge: `Ce fichier dépasse 15${_}Mo.`,
    thanksTitle: `Merci${_}!`,
    thanksText: (vessel: string) =>
      `Votre photo de ${vessel} est dans la file de modération. Elle sera publiée après vérification par un bénévole.`,
    sendAnother: 'Envoyer une autre photo',
    backTo: (vessel: string) => `Retour à ${vessel}`,
    theVessel: 'la fiche du navire',
    title: 'Envoyer une photo',
    lead: 'Votre propre photo, une licence libre, et le CFR ou le MMSI du navire. Elle est publiée après vérification par un bénévole.',
    step1: `1 · Quel navire${_}?`,
    keysHint: 'Remplissez l’un des deux, ou les deux si vous les connaissez.',
    keysConflict: `Ce CFR et ce MMSI sont ceux de deux navires différents${_}: vérifiez-les.`,
    matches: 'Correspond à',
    anUnnamedVessel: 'un navire sans nom',
    noVesselYet: 'Aucun navire avec cet identifiant pour l’instant — il sera créé quand votre photo sera validée.',
    vesselName: 'Nom du navire',
    vesselNamePlaceholder: 'ex. Ker Avel',
    step2: '2 · La photo',
    yourUpload: 'Votre fichier',
    choosePhoto: 'Choisir une photo',
    fileHint: `JPEG ou PNG · navire bien visible · 15${_}Mo maximum`,
    dateTaken: 'Date de prise de vue',
    place: 'Lieu',
    placePlaceholder: 'ex. port de Concarneau',
    step3: '3 · Crédit et licence',
    creditAs: 'Créditer comme',
    creditPlaceholder: 'Votre nom ou pseudonyme',
    licence: 'Licence',
    rights: (licence: string) => `J’ai pris cette photo, ou je détiens le droit de la publier sous ${licence}.`,
    email: 'E-mail',
    emailHint: '(facultatif, visible seulement par les modérateurs s’ils ont une question)',
    submit: 'Envoyer pour vérification',
    noAccount: 'Aucun compte nécessaire.',
    nextTitle: 'Et ensuite',
    nextReceived: 'Reçue.',
    nextReceivedText: 'Votre photo attend dans la file de modération.',
    nextReviewed: 'Vérifiée.',
    nextReviewedText: 'Un bénévole vérifie que le navire correspond à son identifiant et que la licence est claire.',
    nextPublished: 'Publiée.',
    nextPublishedText: 'En ligne sur le site et dans l’API, sous le CFR comme sous le MMSI.',
    tipsTitle: 'Une bonne photo de navire',
    tips: [
      'La coque entière dans le cadre, de profil si possible',
      'Le nom ou l’immatriculation lisible',
      'Pas de visages en gros plan',
      'Uniquement des photos que vous avez prises'
    ],
    exif: 'Les données de localisation (EXIF/GPS) sont supprimées de chaque photo.',
    scripts: `Vous préférez les scripts${_}?`,
    scriptsEnd: 'fait la même chose.'
  },
  apiKey: {
    pageTitle: 'Demander une clé d’API',
    title: 'Demander une clé d’API',
    lead: `La lecture de l’API est libre et gratuite. Une clé d’API n’est nécessaire que pour envoyer des données par script${_}: envoyer des photos, proposer des identifiants ou signaler des photos avec des requêtes POST.`,
    received: 'Demande reçue.',
    receivedText: (email: string) =>
      `Un modérateur enverra votre clé d’API à ${email} une fois votre demande acceptée.`,
    name: 'Votre nom',
    email: 'E-mail',
    emailHint: 'La clé est envoyée à cette adresse.',
    organisation: 'Organisation ou projet',
    use: `À quoi va-t-elle vous servir${_}?`,
    usePlaceholder: 'ex. Importer les photos prises par l’équipe du port, environ 50 par mois.',
    terms:
      'Je n’enverrai que des photos que leurs auteurs ont accepté de publier sous licence libre, avec leur crédit.',
    submit: 'Demander une clé',
    howTitle: 'Comment l’utiliser',
    howHeader: `Envoyez la clé dans l’en-tête x-api-key de vos requêtes POST${_}:`,
    howSecret: `Gardez-la secrète${_}: ne la mettez jamais dans une page web ou une application mobile. Tout ce qui est envoyé reste vérifié avant publication. Voir la`,
    apiDocs: 'documentation de l’API'
  },
  docs: {
    pageTitle: 'API gratuite de photos de navires, par CFR ou MMSI',
    chip: 'API v1 · sans clé',
    title: 'L’API de photos de navires',
    lead: `Le CFR et le MMSI sont deux identifiants à part entière${_}: chaque URL existe sous l’un comme sous l’autre et renvoie le même navire et les mêmes photos. Un navire peut avoir un identifiant ou les deux — un navire hors UE avec seulement un MMSI fonctionne tout aussi bien.`,
    keyCfr: 'Identifiant · CFR (registre de la flotte de l’UE)',
    keyMmsi: 'Identifiant · MMSI (identité radio)',
    endpoints: 'Points d’accès',
    endpointDescriptions: [
      'Le navire, ses deux identifiants, son immatriculation et son port, et toutes ses photos publiées avec crédit et licence.',
      'La photo principale, en image, prête pour une balise <img>. ?size=320|800|full (800 par défaut).',
      `Une photo précise${_}; le même n sous l’un ou l’autre identifiant est le même fichier. Même paramètre ?size.`,
      'Recherche par nom, port, immatriculation, ou début de CFR ou de MMSI. 24 résultats au plus.',
      'Les navires dont les photos ont été publiées le plus récemment. ?limit=12 (48 au plus).',
      `La part de la bibliothèque photographiée${_}: navires du registre de la flotte de l’UE (avec un CFR) et ceux qui ont une photo, tous les navires avec une photo, et le nombre de photos.`,
      `Toutes les données${_}: chaque navire avec les URL, crédits et licences de ses photos.`,
      `Envoyer une photo (multipart${_}: file, credit, licence, takenOn?, place?, email?, vesselName?, et mmsi? sous un CFR ou cfr? sous un MMSI), sous l’identifiant que vous connaissez. Publiée après vérification. Nécessite une clé d’API.`,
      'Proposer l’identifiant manquant, ex. {"keyType": "cfr", "key": "FRA000123456"}. Appliqué après vérification. Nécessite une clé d’API.',
      `Signaler une photo${_}: {"reason": "WRONG_VESSEL|COPYRIGHT|PRIVACY|OTHER", "message": "…"}. Nécessite une clé d’API.`,
      `Demander une clé d’API${_}: {"name", "email", "organisation"?, "intendedUse"}. Envoyée par e-mail une fois acceptée.`
    ],
    example: 'Exemple',
    errorsIntro: 'Les erreurs sont renvoyées sous la forme',
    errorsOutro: 'avec un statut 400, 404 ou 409.',
    apiKeyTitle: 'Clé d’API',
    apiKeyText1: `Tous les GET sont libres, sans clé. Les requêtes POST nécessitent une clé d’API dans l’en-tête x-api-key${_}:`,
    apiKeyLink: 'demandez-en une',
    apiKeyText2: ', c’est gratuit. Sans elle, elles renvoient une erreur 401.',
    rateLimitsTitle: 'Limites de débit',
    rateLimitsText: `Jusqu’à 600${_}GET par minute et par IP, et 1${_}000${_}POST par heure et par clé d’API. Chaque réponse indique où vous en êtes dans X-RateLimit-Limit, X-RateLimit-Remaining et X-RateLimit-Reset (en secondes). Au-delà, les requêtes renvoient une erreur 429 avec un en-tête Retry-After. Besoin de plus${_}? Téléchargez toutes les données depuis /v1/export.json, ou demandez une limite plus haute.`,
    chips: ['CORS activé', 'Lecture sans clé d’API', 'Toujours afficher le crédit'],
    licencesTitle: 'Licences',
    licencesText:
      'Chaque photo est publiée par son auteur sous l’une de ces licences. Réutilisez-la librement, en affichant la ligne de crédit fournie dans attribution quand la licence l’exige.',
    licenceBy: '— réutilisation avec crédit.',
    licenceBySa: '— réutilisation avec crédit, modifications partagées sous la même licence.',
    licenceCc0: '— domaine public, aucun crédit exigé.',
    licenceOlder:
      'Les photos importées de Wikimedia Commons ou de Flickr gardent leur propre licence, qui peut aussi être une version plus ancienne de celles-ci (CC BY ou CC BY-SA 1.0 à 3.0) ou le domaine public.',
    selfHostTitle: 'L’héberger soi-même',
    selfHostText:
      'Le service tient en un conteneur (Spring Boot, qui sert l’API et cette application web) et une base PostgreSQL, qui stocke aussi les images. Renseignez OPENVESSELPHOTOS_DATABASE_URL et OPENVESSELPHOTOS_ADMIN_PASSWORD, puis lancez-le.'
  },
  notFound: {
    title: 'Page introuvable',
    text: 'Cette page n’existe pas.',
    link: 'Rechercher des photos de navires'
  },
  admin: {
    pageTitle: 'Modération',
    queueTitle: 'File de modération',
    statistics: 'Statistiques',
    signOut: 'Se déconnecter',
    loginTitle: 'Modération',
    loginText: 'Connectez-vous avec le mot de passe d’administration pour vérifier les contributions.',
    password: 'Mot de passe d’administration',
    signIn: 'Se connecter',
    queueLabel: 'File',
    tabs: { photos: 'Photos', keyLinks: 'Identifiants', reports: 'Signalements', apiKeys: 'Clés d’API' },
    rejectReasons: [
      'Le navire ne correspond pas à l’identifiant',
      'Ce n’est pas une photo du contributeur',
      'Mauvaise qualité ou navire peu visible',
      'Doublon d’une photo existante',
      'Autre'
    ],
    reportReasons: {
      COPYRIGHT: 'Droit d’auteur / licence',
      OTHER: 'Autre',
      PRIVACY: 'Personnes ou informations privées',
      WRONG_VESSEL: 'Mauvais navire'
    },
    requested: (date: string) => `Demandée le ${date}`,
    organisation: 'Organisation ou projet',
    approveKey: 'Accepter et créer la clé',
    apiKeyRequest: 'demande de clé d’API',
    knownKey: 'Identifiant connu',
    suggestedKey: 'Identifiant proposé',
    suggestedBy: (date: string, by: string) => `Proposé le ${date} par ${by}`,
    anonymous: 'anonyme',
    linkChecks: ['Les deux identifiants figurent dans un registre public', 'Même nom de navire et même pavillon'],
    linkKeys: 'Associer les identifiants',
    addKey: (key: string) => `Ajouter le ${key}`,
    newPhoto: 'Nouvelle photo',
    firstPhoto: 'Première photo · crée le navire',
    reported: (date: string, n: number | null, credit: string, licence: string) =>
      `Signalée le ${date} · photo ${n} de ${credit} (${licence})`,
    reason: 'Motif',
    removePhoto: 'Retirer la photo',
    dismissReport: 'Classer le signalement',
    reportSubtitle: (reason: string, n: number | null) => `Signalement · ${reason} · photo ${n}`,
    nothingToReview: 'Rien à vérifier ici.',
    pendingItems: 'Éléments en attente',
    newVessel: 'Nouveau navire',
    submitted: (date: string, by: string) => `Envoyée le ${date} par ${by}`,
    sentWithKey: (name: string) => ` · envoyée avec la clé d’API de ${name}`,
    submittedKey: 'Identifiant envoyé',
    licence: 'Licence',
    taken: 'Prise',
    size: 'Taille',
    vesselName: 'Nom du navire',
    photoChecks: [
      'Le navire de la photo correspond à l’identifiant',
      'Le contributeur l’a vraisemblablement prise lui-même',
      'Pas de visages en gros plan ni d’informations privées',
      'Pas un doublon'
    ],
    checklist: 'Points à vérifier',
    rejectWhy: `En cas de refus, pourquoi${_}?`,
    approvePhoto: 'Accepter et publier',
    previewAlt: 'Aperçu de la contribution',
    keyCreatedFor: (name: string) => `Clé d’API créée pour ${name}`,
    sendItNow: `Envoyez-la maintenant${_}: elle ne sera plus affichée.`,
    emailIt: (email: string) => `L’envoyer à ${email}`,
    copyKey: 'Copier la clé',
    done: 'Terminé',
    emailSubject: 'Votre clé d’API Open Vessel Photos',
    emailBody: (name: string, key: string, docsUrl: string) =>
      `Bonjour ${name},\n\nVotre demande de clé d’API a été acceptée. Voici votre clé${_}:\n\n${key}\n\nEnvoyez-la dans l’en-tête x-api-key de vos requêtes POST, et gardez-la secrète. Documentation${_}: ${docsUrl}\n`,
    activeKeys: 'Clés d’API actives',
    noActiveKey: 'Aucune clé active.',
    keyColumns: {
      name: 'Nom',
      email: 'E-mail',
      key: 'Clé',
      created: 'Créée le',
      lastUsed: 'Dernière utilisation',
      actions: 'Actions'
    },
    revoke: 'Révoquer',
    importTitle: 'Importer des navires',
    importText1: 'Crée des navires sans photo, pour que leur page existe et puisse être trouvée. Accepte un export du',
    importRegister: 'registre de la flotte de l’UE',
    importText2: `(CSV, dernier état des navires d’un pays${_}: ceux sortis de flotte, détruits ou exportés sont écartés), ou un CSV avec les colonnes cfr, mmsi, name, registration, port. Les navires existants ne sont jamais écrasés${_}: seules leurs données manquantes sont complétées. 15${_}Mo maximum.`,
    csvFile: 'Fichier CSV',
    importing: 'Import…',
    import: 'Importer',
    photoImportTitle: 'Importer des photos',
    photoImportText: `Publie sans relecture des photos sous licence libre, venant par exemple de Wikimedia Commons. Colonnes du CSV${_}: mmsi et/ou cfr, vessel_name, author, licence, file_page (page de la photo d’origine), image_1600_url et, en option, date. Les images sont téléchargées depuis Wikimedia Commons ou Flickr une à une, en arrière-plan${_}: cela peut prendre quelques heures. Les photos déjà importées sont ignorées${_}: le même fichier peut être renvoyé après une interruption.`,
    photoImportRunning: 'Import en cours…',
    photoImportFinished: 'Dernier import terminé.',
    photoImportProgress: s =>
      `${s.done} / ${s.total} photos${_}: ${s.imported} importées, ${s.alreadyImported} déjà présentes, ${s.failed} en échec. ${s.invalid} lignes invalides ignorées.`,
    stopImport: 'Arrêter',
    importResult: r =>
      `${r.read}${_}lignes lues${_}: ${r.created}${_}navires créés, ${r.updated}${_}complétés, ${r.unchanged}${_}déjà à jour, ${r.outOfFleet}${_}sortis de la flotte, ${r.skipped}${_}ignorés.`
  },
  stats: {
    pageTitle: 'Statistiques',
    title: 'Statistiques',
    backToQueue: '← File de relecture',
    period: 'Période',
    lastDays: days => (days === 365 ? 'Dernière année' : `${days}${_}derniers jours`),
    imageViews: 'Vues des photos',
    imageViewsElsewhere: (count, sites) =>
      `${count} sur ${sites} autre${sites > 1 ? 's' : ''} site${sites > 1 ? 's' : ''}`,
    apiCalls: 'Appels à l’API',
    apiCallsWithKey: count => `${count} avec une clé d’API`,
    activeKeys: 'Clés d’API actives',
    activeKeysHint: 'Utilisées sur la période / toutes les clés actives',
    photos: 'Photos publiées',
    vessels: count => `${count}${_}navires`,
    imageViewsPerDay: 'Vues des photos par jour',
    apiCallsPerDay: 'Appels à l’API par jour',
    vesselsAddedPerDay: total => `Navires ajoutés par jour (${total} au total, imports compris)`,
    imageSources: 'Sites qui affichent les photos',
    apiSources: 'Appels à l’API par site',
    apiKeys: 'Appels à l’API par clé',
    noKeyCalls: 'Aucun appel avec une clé d’API pour l’instant.',
    noData: 'Rien de compté pour l’instant.',
    site: 'Site',
    count: 'Nombre',
    key: 'Clé',
    calls: 'Appels',
    lastUsed: 'Dernière utilisation',
    website: 'Ce site',
    direct: 'Applications et scripts (sans référent)',
    other: 'Autres sites',
    note: `Compté sans donnée personnelle${_}: les images par le site qui les affiche (en-tête Referer), les appels à l’API par clé. Un navigateur garde une photo un jour${_}: elle est comptée au plus une fois par jour et par visiteur${_}; la dernière minute n’est pas encore comptée.`
  }
}
