package com.hotelmanagement.hms.identity.membership.service;

import com.hotelmanagement.hms.identity.membership.dto.HotelMembershipResponse;
import com.hotelmanagement.hms.identity.membership.model.HotelMembership;
import com.hotelmanagement.hms.identity.membership.model.MembershipBranchAccess;
import com.hotelmanagement.hms.identity.membership.repository.HotelMembershipRepository;
import com.hotelmanagement.hms.identity.membership.repository.MembershipBranchAccessRepository;
import com.hotelmanagement.hms.identity.model.UserAccount;
import com.hotelmanagement.hms.identity.repository.UserRepository;
import com.hotelmanagement.hms.platform.model.Branch;
import com.hotelmanagement.hms.platform.model.Hotel;
import com.hotelmanagement.hms.platform.repository.BranchRepository;
import com.hotelmanagement.hms.platform.repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class HotelMembershipService {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final BranchRepository branchRepository;

    private final HotelMembershipRepository membershipRepository;

    private final MembershipBranchAccessRepository
            branchAccessRepository;

    public HotelMembershipService(
            UserRepository userRepository,
            HotelRepository hotelRepository,
            BranchRepository branchRepository,
            HotelMembershipRepository membershipRepository,
            MembershipBranchAccessRepository branchAccessRepository) {

        this.userRepository = userRepository;
        this.hotelRepository = hotelRepository;
        this.branchRepository = branchRepository;
        this.membershipRepository = membershipRepository;
        this.branchAccessRepository = branchAccessRepository;
    }

    /**
     * Adds an existing HMS user account to a hotel.
     *
     * A single user may belong to several hotels, but may have only
     * one membership record for a particular hotel.
     */
    @Transactional
    public HotelMembershipResponse createMembership(
            UUID userId,
            UUID hotelId,
            boolean allBranches) {

        UserAccount user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found: " + userId));

        Hotel hotel =
                hotelRepository.findById(hotelId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Hotel not found: " + hotelId));

        if (membershipRepository
                .existsByUser_IdAndHotel_Id(
                        userId,
                        hotelId)) {

            throw new IllegalStateException(
                    "The user already has a membership "
                            + "for this hotel.");
        }

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        HotelMembership membership =
                HotelMembership.create(
                        user,
                        hotel,
                        allBranches,
                        now
                );

        HotelMembership savedMembership =
                membershipRepository.saveAndFlush(
                        membership);

        return HotelMembershipResponse.from(
                savedMembership);
    }

    /**
     * Returns all memberships belonging to one hotel.
     */
    @Transactional(readOnly = true)
    public List<HotelMembershipResponse> getHotelMemberships(
            UUID hotelId) {

        ensureHotelExists(hotelId);

        return membershipRepository
                .findByHotel_IdOrderByCreatedAtAsc(hotelId)
                .stream()
                .map(HotelMembershipResponse::from)
                .toList();
    }

    /**
     * Grants access to one specific branch.
     *
     * This operation is meaningful only when the membership does
     * not already have access to every branch.
     */
    @Transactional
    public void grantBranchAccess(
            UUID hotelId,
            UUID membershipId,
            UUID branchId) {

        HotelMembership membership =
                getMembership(
                        membershipId,
                        hotelId);

        if (membership.hasAllBranches()) {
            throw new IllegalStateException(
                    "This membership already has access "
                            + "to all hotel branches.");
        }

        Branch branch =
                branchRepository
                        .findByIdAndHotel_Id(
                                branchId,
                                hotelId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Branch not found in this hotel: "
                                                + branchId));

        if (branchAccessRepository
                .existsByMembership_IdAndBranch_Id(
                        membershipId,
                        branchId)) {

            throw new IllegalStateException(
                    "The membership already has access "
                            + "to this branch.");
        }

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        MembershipBranchAccess branchAccess =
                MembershipBranchAccess.create(
                        membership,
                        branch,
                        now
                );

        branchAccessRepository.saveAndFlush(
                branchAccess);
    }

    /**
     * Removes one explicit branch grant.
     *
     * Removing an authorization mapping is allowed because this is
     * access-control state, not an immutable financial transaction.
     */
    @Transactional
    public void revokeBranchAccess(
            UUID hotelId,
            UUID membershipId,
            UUID branchId) {

        HotelMembership membership =
                getMembership(
                        membershipId,
                        hotelId);

        if (membership.hasAllBranches()) {
            throw new IllegalStateException(
                    "Specific branch access cannot be revoked "
                            + "while the membership has "
                            + "all-branch access.");
        }

        MembershipBranchAccess access =
                branchAccessRepository
                        .findByMembership_IdAndBranch_Id(
                                membershipId,
                                branchId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "The membership does not have "
                                                + "access to this branch."));

        /*
         * Database constraints already guarantee that a branch
         * grant belongs to the membership's hotel.
         */
        branchAccessRepository.delete(access);
    }

    /**
     * Grants access to all current and future branches.
     *
     * Explicit branch grants become redundant and are removed.
     */
    @Transactional
    public HotelMembershipResponse grantAllBranches(
            UUID hotelId,
            UUID membershipId) {

        HotelMembership membership =
                getMembership(
                        membershipId,
                        hotelId);

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        membership.grantAllBranches(now);

        branchAccessRepository
                .deleteByMembership_Id(
                        membershipId);

        HotelMembership savedMembership =
                membershipRepository.saveAndFlush(
                        membership);

        return HotelMembershipResponse.from(
                savedMembership);
    }

    /**
     * Changes a membership from hotel-wide branch access to
     * explicit branch access.
     *
     * After this operation the user has no branch access until
     * specific branch grants are added.
     */
    @Transactional
    public HotelMembershipResponse restrictToSelectedBranches(
            UUID hotelId,
            UUID membershipId) {

        HotelMembership membership =
                getMembership(
                        membershipId,
                        hotelId);

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        membership.restrictToSelectedBranches(
                now);

        HotelMembership savedMembership =
                membershipRepository.saveAndFlush(
                        membership);

        return HotelMembershipResponse.from(
                savedMembership);
    }

    @Transactional
    public HotelMembershipResponse suspendMembership(
            UUID hotelId,
            UUID membershipId) {

        HotelMembership membership =
                getMembership(
                        membershipId,
                        hotelId);

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        membership.suspend(now);

        HotelMembership savedMembership =
                membershipRepository.saveAndFlush(
                        membership);

        return HotelMembershipResponse.from(
                savedMembership);
    }

    @Transactional
    public HotelMembershipResponse activateMembership(
            UUID hotelId,
            UUID membershipId) {

        HotelMembership membership =
                getMembership(
                        membershipId,
                        hotelId);

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        membership.activate(now);

        HotelMembership savedMembership =
                membershipRepository.saveAndFlush(
                        membership);

        return HotelMembershipResponse.from(
                savedMembership);
    }

    /**
     * Revokes access to the hotel but keeps the membership record
     * for historical and audit purposes.
     */
    @Transactional
    public HotelMembershipResponse revokeMembership(
            UUID hotelId,
            UUID membershipId) {

        HotelMembership membership =
                getMembership(
                        membershipId,
                        hotelId);

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        membership.revoke(now);

        HotelMembership savedMembership =
                membershipRepository.saveAndFlush(
                        membership);

        return HotelMembershipResponse.from(
                savedMembership);
    }

    private HotelMembership getMembership(
            UUID membershipId,
            UUID hotelId) {

        return membershipRepository
                .findByIdAndHotel_Id(
                        membershipId,
                        hotelId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Membership not found in this hotel: "
                                        + membershipId));
    }

    private void ensureHotelExists(
            UUID hotelId) {

        if (!hotelRepository.existsById(hotelId)) {
            throw new IllegalArgumentException(
                    "Hotel not found: " + hotelId);
        }
    }
}