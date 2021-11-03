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
          'Tutorial': [
            'tutorial/00-introduction',
            'tutorial/01-configure-project',
            'tutorial/02-add-the-graphql-schema',
            'tutorial/03-write-your-first-query',
            'tutorial/04-execute-the-query',
            'tutorial/05-connect-queries-to-your-ui',
            'tutorial/06-add-more-info',
            'tutorial/07-paginate-results',
            'tutorial/08-add-a-details-view',
            'tutorial/09-write-your-first-mutation',
            'tutorial/10-authenticate-your-queries',
            'tutorial/11-subscriptions',
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
            'advanced/response-based-codegen',
          ],
        }
      }
    }
  ]
};
