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
            'essentials/file-types',
            'essentials/queries',
            'essentials/mutations',
            'essentials/subscriptions',
            'essentials/errors',
            'essentials/custom-scalars',
            'essentials/inline-fragments',
            'essentials/named-fragments',
          ],
          Caching: [
            'caching/introduction',
            'caching/normalized-cache',
            'caching/declarative-ids',
            'caching/programmatic-ids',
            'caching/query-watchers',
            'caching/store',
            'caching/http-cache',
            'caching/troubleshooting',
          ],
          Advanced: [
            'advanced/plugin-configuration',
            'advanced/multi-modules',
            'advanced/persisted-queries',
            'advanced/using-aliases',
            'advanced/response-based-codegen',
            'advanced/client-awareness',
            'advanced/interceptors-http',
            'advanced/http-engine',
            'advanced/query-batching',
            'advanced/upload',
            'advanced/no-runtime',
            'advanced/nonnull',
            'advanced/java',
            'advanced/rxjava',
            'advanced/ui-tests',
            'advanced/kotlin-native',
            'advanced/apollo-ast',
            'advanced/test-builders',
          ],
        }
      }
    }
  ]
};
