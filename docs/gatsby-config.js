const themeOptions = require('gatsby-theme-apollo-docs/theme-options');

module.exports = {
  pathPrefix: '/docs/android',
  plugins: [
    {
      resolve: 'gatsby-theme-apollo-docs',
      options: {
        ...themeOptions,
        root: __dirname,
        subtitle: 'Client (Kotlin / Android)',
        description: 'A guide to using Apollo with Kotlin and Android',
        githubRepo: 'apollographql/apollo-android',
        sidebarCategories: {
          null: [
            'index',
            'get-started',
            'migration/3.0',
            '[Kdoc](https://apollographql.github.io/apollo-android/kdoc/)',
          ],
          'Essentials': [
            'essentials/00-queries',
            'essentials/10-mutations',
            'essentials/20-subscriptions',
            'essentials/01-errors',
            'essentials/30-custom-scalars',
            'essentials/40-inline-fragments',
            'essentials/50-named-fragments',
            'essentials/60-plugin-configuration',
          ],
          Advanced: [
            'advanced/client-awareness',
            'advanced/interceptors-http',
            'advanced/http-engine',
            'advanced/no-runtime',
            'advanced/multi-modules',
            'advanced/nonnull',
            'advanced/persisted-queries',
            'advanced/upload',
            'advanced/using-aliases',
          ],
        }
      }
    }
  ]
};
