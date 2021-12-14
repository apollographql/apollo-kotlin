const themeOptions = require('gatsby-theme-apollo-docs/theme-options');

module.exports = {
  plugins: [
    {
      resolve: 'gatsby-theme-apollo-docs',
      options: {
        ...themeOptions,
        root: __dirname,
        pathPrefix: '/docs/kotlin',
        algoliaIndexName: 'android',
        algoliaFilters: ['docset:android'],
        subtitle: 'Client (Kotlin)',
        description: 'A guide to using Apollo with Kotlin and Android',
        githubRepo: 'apollographql/apollo-android',
        defaultVersion: '3',
        versions: {
          '2': 'release-2.x',
        },
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
            'advanced/operation-variables',
            'essentials/errors',
            'essentials/custom-scalars',
            'essentials/fragments',
          ],
          Caching: [
            'caching/introduction',
            'caching/normalized-cache',
            'caching/declarative-ids',
            'caching/programmatic-ids',
            'caching/query-watchers',
            'caching/store',
            'caching/http-cache',
            'advanced/persisted-queries',
            'caching/troubleshooting',
          ],
          Networking: [
            'advanced/interceptors-http',
            'advanced/http-engine',
            'advanced/no-runtime',
          ],
          Testing: [
            'advanced/ui-tests',
            'advanced/test-builders',
          ],
          Advanced: [
            'advanced/query-batching',
            'advanced/upload',
            'advanced/nonnull',
            'advanced/plugin-configuration',
            'advanced/multi-modules',
            'advanced/operation-safelisting',
            'advanced/using-aliases',
            'advanced/client-awareness',
            'advanced/response-based-codegen',
            'advanced/java',
            'advanced/rxjava',
            'advanced/kotlin-native',
            'advanced/apollo-ast',
          ],
        }
      }
    }
  ]
};
