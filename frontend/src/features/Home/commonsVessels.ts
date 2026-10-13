import julienColeou from '../../assets/commons/julien-coleou.jpg'
import lesMenhirs from '../../assets/commons/les-menhirs.jpg'
import miren from '../../assets/commons/miren.jpg'

export type CommonsVessel = {
  author: string
  fileUrl: string
  licence: string
  licenceUrl: string
  name: string
  photo: string
  /** French fishing licence number, as painted on the hull */
  registration: string
}

/**
 * Real fishing vessels of Lorient, photographed on Wikimedia Commons (photos cropped). Shown on the home chart while
 * the library has fewer than 3 vessels of its own.
 */
export const COMMONS_VESSELS: CommonsVessel[] = [
  {
    author: 'Bleizmor',
    fileUrl: 'https://commons.wikimedia.org/wiki/File:Les_Menhirs.JPG',
    licence: 'CC BY-SA 4.0',
    licenceUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
    name: 'Les Menhirs',
    photo: lesMenhirs,
    registration: 'LO 752221'
  },
  {
    author: 'Pline',
    fileUrl: 'https://commons.wikimedia.org/wiki/File:Chalutier-Julien-Coleou.jpg',
    licence: 'CC BY-SA 3.0',
    licenceUrl: 'https://creativecommons.org/licenses/by-sa/3.0/',
    name: 'Julien Coléou',
    photo: julienColeou,
    registration: 'LO 911288'
  },
  {
    author: 'Bleizmor',
    fileUrl: 'https://commons.wikimedia.org/wiki/File:Miren.JPG',
    licence: 'CC BY-SA 4.0',
    licenceUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
    name: 'Miren',
    photo: miren,
    registration: 'LO 425329'
  }
]
