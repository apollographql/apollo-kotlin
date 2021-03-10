//
//  RepositoryManager.swift
//  kmp-ios-app
//
//  Created by Ellen Shapiro on 4/26/20.
//

import Foundation
import kmp_lib_sample
import SwiftUI

class RepositoryManager: ObservableObject {
    
    private let repository = ApolloiOSRepositoryKt.create()
    
    @Published var repos: [RepositoryFragmentImpl] = []
    @Published var repoDetails: [String: RepositoryDetail] = [:]
    @Published var commits: [String: [GithubRepositoryCommitsQueryDataViewerRepositoryRefTargetCommitTarget.HistoryEdgesNode]] = [:]
    
    func fetch() {
        self.repository.fetchRepositories { [weak self] repos in
            self?.repos = repos as [RepositoryFragmentImpl]
        }
    }
    
    func fetchDetails(for repo: RepositoryFragment) {        
        self.repository.fetchRepositoryDetail(repositoryName: repo.name) { [weak self] detail in
            if let detail = detail {
                self?.repoDetails[repo.name] = detail
            }
        }
    }
    
    func fetchCommits(for repo: RepositoryFragment) {
        // NOTE: This comes in as `[Any]` due to some some issues with representing
        // optional types in a generic array in Objective-C from K/N. The actual
        // type coming back here is `GithubRepositoryCommitsQuery.Edge`, and the
        // `Node` it contains is where actual information about the commit lives.
        self.repository.fetchCommits(repositoryName: repo.name) { [weak self] commits in
            let filteredCommits = commits
                .compactMap { $0 as? GithubRepositoryCommitsQueryDataViewerRepositoryRefTargetCommitTarget.HistoryEdges }
                .compactMap { $0.node }
            self?.commits[repo.name] = filteredCommits
        }
    }
}
