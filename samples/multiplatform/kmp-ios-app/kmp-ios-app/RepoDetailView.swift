//
//  RepoDetailView.swift
//  kmp-ios-app
//
//  Created by Ellen Shapiro on 4/26/20.
//

import SwiftUI
import kmp_lib_sample

struct RepoDetailView: View {
    
    let repo: RepositoryFragment
    @ObservedObject var repoManager: RepositoryManager
    
    var body: some View {
        List {
            Text(self.repoDescription)
            Text("🌟 (stars): \(self.stargazers)")
            Text("🍴 (forks): \(self.forks)")
            Text("↔️ (pull requests): \(self.pulls)")
            Text("😭 (issues): \(self.issues)")
            Text("🏷 (releases): \(self.releases)")
            NavigationLink(destination:
                CommitListView(repoManager: self.repoManager, repo: self.repo).onAppear {
                    self.repoManager.fetchCommits(for: self.repo)
                }
            ) {
                Text("Commits")
            }
        }.navigationBarTitle(Text(self.repo.name), displayMode: .inline)
    }
    
    var details: RepositoryDetail? {
        repoManager.repoDetails[repo.name]
    }
    
    var repoDescription: String {
        if let description = details?.repoDescription {
            return description
        } else {
            return "..."
        }
    }
    
    var stargazers: String {
        if let count = details?.stargazers.totalCount {
            return "\(count)"
        } else {
            return "..."
        }
    }
    
    var forks: String {
        if let forks = details?.forkCount {
            return "\(forks)"
        } else {
            return "..."
        }
    }
    
    var pulls: String {
        if let pulls = details?.pullRequests.totalCount {
            return "\(pulls)"
        } else {
            return "..."
        }
    }
    
    var issues: String {
        if let issues = details?.issues.totalCount {
            return "\(issues)"
        } else {
            return "..."
        }
    }
    
    var releases: String {
        if let releases = details?.releases.totalCount {
            return "\(releases)"
        } else {
            return "..."
        }
    }
}

struct RepoDetailView_Previews: PreviewProvider {
    static var previews: some View {
        let manager = RepositoryManager()
        let repo = RepositoryFragmentImpl.Data(__typename: "__typename",
                                      id: "1",
                                      name: "TestRepo",
                                      repoDescription: "a test repo")
        manager.repoDetails[repo.name] = RepositoryDetailImpl.Data(__typename: "__typename",
                                                          id: "1",
                                                          name: "Test Repo", repoDescription: "A Test Repo", issues: RepositoryDetailImpl.DataIssues(totalCount: 3),
                                                          pullRequests: RepositoryDetailImpl.DataPullRequests( totalCount: 1),
                                                          stargazers: RepositoryDetailImpl.DataStargazers( totalCount: 25),
                                                          forkCount: 2,
                                                          releases: RepositoryDetailImpl.DataReleases(totalCount: 14))
        return NavigationView {
            RepoDetailView(repo: repo,
                           repoManager: manager)
        }
    }
}
