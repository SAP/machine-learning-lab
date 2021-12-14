import { initReactI18next } from 'react-i18next';
import HttpApi from 'i18next-http-backend';
import LanguageDetector from 'i18next-browser-languagedetector';
import i18n from 'i18next';

import en from './en.json';

i18n
  .use(HttpApi)
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    interpolation: {
      // React already does escaping
      escapeValue: false,
    },
    defaultNS: 'translation',
    fallbackNS: 'translation',
    ns: ['translation'],
    lng: 'en', // 'en' | 'de'
    fallbackLng: 'en',
    backend: {},
  });

// Pre-bundle the app with the English language and avoid an XHR request. See this discussion: https://github.com/i18next/i18next/issues/617
i18n.addResourceBundle('en', 'translation', en);

export default i18n;
