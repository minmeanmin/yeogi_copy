package com.example.final_project.admin;

import com.example.final_project._core.enums.CompanyEnum;
import com.example.final_project._core.enums.ReportEnum;
import com.example.final_project._core.enums.ReviewEnum;
import com.example.final_project._core.enums.UserEnum;
import com.example.final_project._core.errors.exception.Exception400;
import com.example.final_project._core.errors.exception.Exception401;
import com.example.final_project._core.errors.exception.Exception404;
import com.example.final_project.company.Company;
import com.example.final_project.company.CompanyRepository;
import com.example.final_project.company.CompanyRequest;
import com.example.final_project.company.SessionCompany;
import com.example.final_project.faq.Faq;
import com.example.final_project.faq.FaqRepository;
import com.example.final_project.pay.Pay;
import com.example.final_project.pay.PayRepository;
import com.example.final_project.pay.PayResponse;
import com.example.final_project.question.Question;
import com.example.final_project.question.QuestionRepository;
import com.example.final_project.report.Report;
import com.example.final_project.report.ReportRepository;
import com.example.final_project.reservation.Reservation;
import com.example.final_project.reservation.ReservationRepository;
import com.example.final_project.review.Review;
import com.example.final_project.review.ReviewRepository;
import com.example.final_project.review.ReviewResponse;
import com.example.final_project.room.RoomRepository;
import com.example.final_project.stay.Stay;
import com.example.final_project.stay.StayRepository;
import com.example.final_project.stay_image.StayImageRepository;
import com.example.final_project.user.SessionUser;
import com.example.final_project.user.User;
import com.example.final_project.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PayRepository payRepository;
    private final CompanyRepository companyRepository;
    private final ReviewRepository reviewRepository;
    private final StayRepository stayRepository;
    private final RoomRepository roomRepository;
    private final StayImageRepository stayImageRepository;
    private final QuestionRepository questionRepository;
    private final ReportRepository reportRepository;
    private final FaqRepository faqRepository;


    //로그인
    @Transactional
    public SessionAdmin login(AdminRequest.LoginDTO reqDTO){

        Admin sessionUser = adminRepository.findByIdAndPassword(reqDTO.getName(),reqDTO.getPassword())
                .orElseThrow(() -> new Exception404("해당 관리자를 찾을 수 없습니다"));


        return new SessionAdmin(sessionUser);
    }


    // 모든 유저 정보 리스트
    public List<AdminResponse.UserListDTO> adminUserList() {
        List<User> userList = userRepository.findAll();

        List<AdminResponse.UserListDTO> respDTO = userList.stream().map(AdminResponse.UserListDTO::new).collect(Collectors.toList());

        return respDTO;
    }

    // 특정 기업의 정보 상세보기
    public AdminResponse.UserDetailDTO adminUserDetail(Integer userId) {
        Optional<User> userOP = userRepository.findById(userId);
        User user = null;
        if(userOP.isPresent()){
            user = userOP.get();
        }
        return new AdminResponse.UserDetailDTO(user);
    }

    // 블랙 리스트에 추가 (개인)
    public SessionUser addUserToBlackList(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("존재 하지 않는 계정입니다"));
        user.setState(UserEnum.BLACK);
        userRepository.save(user);
        return new SessionUser(user);
    }

    // 블랙 리스트에서 삭제 (개인)
    public SessionUser removeUserFromBlackList(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("존재 하지 않는 계정입니다"));
        user.setState(UserEnum.ACTIVE);
        userRepository.save(user);
        return new SessionUser(user);
    }

    // 개인 회원을 클릭했을 때, 그 회원의 예약 정보 리스트
    public List<AdminResponse.UserReservationDTO> adminReservationList(Integer userId) {
        List<Reservation> reservationList = reservationRepository.findByUserIdWithRoomAndStay(userId);
        Pay pay = null;

        return reservationList.stream().map(r -> {
            Optional<Pay> payOP = payRepository.findByReservationId(r.getId());
            return new AdminResponse.UserReservationDTO(r, r.getRoom(), payOP.get());
        }).collect(Collectors.toList());
    }


    // 특정 회원의 예약 정보 상세보기
    public AdminResponse.UserReservationDetailDTO adminReservationDetailList(Integer reservationId) {
        Reservation reservation = reservationRepository.findByReservationIdWithRoomAndStay(reservationId);
        Optional<Pay> payOP = payRepository.findByReservationId(reservation.getId());
        Pay pay = null;
        if (payOP.isPresent()) pay = payOP.get();
        AdminResponse.UserReservationDetailDTO respDTO = new AdminResponse.UserReservationDetailDTO(reservation, reservation.getRoom(), pay);

        return new AdminResponse.UserReservationDetailDTO(reservation, reservation.getRoom(), pay);
    }

    // 모든 기업 정보 리스트
    public List<AdminResponse.CompanyListDTO> adminCompanyList() {
        List<Company> companyList = companyRepository.findAll();
        return companyList.stream().map(AdminResponse.CompanyListDTO::new).collect(Collectors.toList());
    }

    // 키워드 search
    public List<AdminResponse.CompanyKeywordList> serarchCompanyKeyword(String keyword){
        List<Company> companyList;
        if (keyword.isBlank()){
            companyList = companyRepository.findAll();
        } else{
            companyList = companyRepository.findAllKeyword(keyword);
        }
        return companyList.stream().map(AdminResponse.CompanyKeywordList::new).toList();
    }

    // 키워드 search 유저
    public List<AdminResponse.UserListDTO> adminSearchUserList(String keyword){
        List<User> userList;
        if (keyword.isBlank()){
            userList = userRepository.findAll();
        }else {
            userList =  userRepository.findAllKeyword(keyword);
        }
        return userList.stream().map(AdminResponse.UserListDTO::new).toList();
    }
    @Transactional
    //키워드 search 신고
    public List<AdminResponse.ReportList> adminsearchReportList(String keyword){
        List<Report> reportList;

        if (keyword.isBlank()){
            reportList = reportRepository.findAllWithReviewAndUserAndStay();
        }else {
            reportList = reportRepository.findAllKeyword(keyword);
        }

        return reportList.stream().map(report -> new AdminResponse.ReportList(report,report.getReview())).toList();
    }

    // 특정 기업의 정보 상세보기
    public AdminResponse.CompanyDetailDTO adminCompanyDetail(Integer companyId) {
        Optional<Company> companyOP = companyRepository.findById(companyId);
        Company company = null;
        if(companyOP.isPresent()){
            company = companyOP.get();
        }
        return new AdminResponse.CompanyDetailDTO(company);
    }

    // 블랙 리스트에 추가 (기업)
    public SessionCompany addCompanyToBlackList(Integer companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception404("존재 하지 않는 계정입니다"));
        company.setState(CompanyEnum.BLACK);
        companyRepository.save(company);
        return new SessionCompany(company);
    }

    // 블랙 리스트에서 삭제 (기업)
    public SessionCompany removeCompanyFromBlackList(Integer companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception404("존재 하지 않는 계정입니다"));
        company.setState(CompanyEnum.ACTIVE);
        companyRepository.save(company);
        return new SessionCompany(company);
    }

    // 특정 개인이 쓴 리뷰 내역 조회
    public List<Review> adminUserReviewList(Integer userId) {
        return reviewRepository.findByUserIdWithUserAndRoom(userId);
    }

    // 기업 가입 거절
    @Transactional
    public void rejectCompany(Integer companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception404("존재 하지 않는 계정입니다"));
        company.setState(CompanyEnum.REJECT);
        companyRepository.save(company);
    }

    // 기업 가입 승인
    @Transactional
    public void activeCompany(Integer companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception404("존재 하지 않는 계정입니다"));
        company.setState(CompanyEnum.ACTIVE);
        companyRepository.save(company);
    }

//    // 기업 수익 전체 조회
//    public PayResponse.TotalIncomeDTO findIncomeByStayAndTotalIncome(SessionCompany sessionCompany) {
//        Company company = companyRepository.findById(sessionCompany.getId())
//                .orElseThrow(() -> new Exception404("존재 하지 않는 계정입니다"));
//
//        // 전체 수익 가져오기
//        PayResponse.TotalIncomeDTO respDTO = payRepository.findTotalIncome(company.getId());
//
//        // 만약 수익이 전혀 없으면 0을 반환
//        if (respDTO == null) {
//            respDTO = new PayResponse.TotalIncomeDTO(company.getId(), 0L, 0L);
//        }
//
//        return respDTO;
//    }

//    // 숙소 수익 전체 조회
//    public PayResponse.TotalIncomeDTO findIncomeByStayAndTotalIncome(SessionCompany sessionCompany, Integer stayId) {
//        Company company = companyRepository.findById(sessionCompany.getId())
//                .orElseThrow(() -> new Exception404("존재 하지 않는 계정입니다"));
//        //Stay stay = stayRepository.findById()
//
//        // 전체 수익 가져오기
//        PayResponse.TotalIncomeDTO respDTO = payRepository.findTotalIncome(company.getId());
//
//        // 만약 수익이 전혀 없으면 0을 반환
//        if (respDTO == null) {
//            //respDTO = new PayResponse.StayTotalIncomeDTO(company.getId(), ,0L, 0L);
//        }
//
//        return respDTO;
//    }

//    // 개인이 작성한 리뷰 정보 리스트
//    public List<AdminResponse.userReviewListDTO> userReviewList (Integer userId) {
//        List<Review> reviewList = reviewRepository.findByUserIdWithUserAndRoom(userId);
//        List<AdminResponse.userReviewListDTO> respDTO = reviewList.stream().map(review -> {
//            return new AdminResponse.userReviewListDTO(review);
//        }).collect(Collectors.toList());
//        return respDTO;
//    }

    // 신고받은 리뷰 찾기
    public List<AdminResponse.ReportList> reportedReviewList(){
        List<Report> reportList = reportRepository.findAllWithReviewAndUserAndStay();
        return reportList.stream().map(report -> {
            return new AdminResponse.ReportList(report, report.getReview());
        }).toList();
    }

    // 신고받은 리뷰 중 하나 상세보기
    @Transactional
    public AdminResponse.ReportDetail reportedReviewDetail(Integer reportedId){
        // 1. 신고 조회
        Report report = reportRepository.findByIdWithReviewAndUserAndStay(reportedId);

        // 2. 리뷰 조회
        Review review = reviewRepository.findByReviewId(report.getReview().getId());

        // 3. 리뷰 작성자 정보 생성
        ReviewResponse.Detail.UserDTO writerDTO = null;
        if (review.getUser() != null) {
            writerDTO = new ReviewResponse.Detail.UserDTO(review.getUser());
        } else if (review.getCompany() != null) {
            writerDTO = new ReviewResponse.Detail.UserDTO(review.getCompany());
        }

        // 4. 리뷰 디테일 정보 생성
        ReviewResponse.Detail detail = new ReviewResponse.Detail(review, writerDTO);

        // 5. 자식 리뷰 리스트 구성
        for (Review childReview : review.getChildren()) {
            Hibernate.initialize(childReview.getStay().getOptions()); // 자식 리뷰의 options 컬렉션 초기화
            ReviewResponse.Detail.UserDTO childWriterDTO = null;
            if (childReview.getUser() != null) { // 유저 비었을 때
                childWriterDTO = new ReviewResponse.Detail.UserDTO(childReview.getUser());
            } else if (childReview.getCompany() != null) { // 기업 비었을 때
                childWriterDTO = new ReviewResponse.Detail.UserDTO(childReview.getCompany());
            }

            ReviewResponse.Detail childDetail = new ReviewResponse.Detail(childReview, childWriterDTO);
            detail.getChildren().add(childDetail);
        }
        return new AdminResponse.ReportDetail(report, detail);
    }

    // 신고 적합(승인)
    @Transactional
    public void reportApproval(Integer reportId, Integer reviewId){
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new Exception404("존재 하지 않는 신고입니다"));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new Exception404("존재하지 않는 리뷰입니다."));

        report.setResult(ReportEnum.COMPLETE);
        reportRepository.save(report);

        review.setState(ReviewEnum.REPORTED);
        reviewRepository.save(review);
    }

    // 신고 부적합(거절)
    @Transactional
    public void reportRefuse(Integer reportId, Integer reviewId){
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new Exception404("존재 하지 않는 신고입니다"));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new Exception404("존재하지 않는 리뷰입니다."));

        report.setResult(ReportEnum.FAIL);
        reportRepository.save(report);

        review.setState(ReviewEnum.FLAWLESS);
        reviewRepository.save(review);
    }

    // 관리자 페이지에서 특정 기업의 숙소 정보 출력
    public List<AdminResponse.CompanyStayListDTO> adminCompanyStayList(Integer companyId){
        List<Stay> stayList = stayRepository.findByCompanyId(companyId);

        return stayList.stream().map(stay -> {
            Integer roomsSize = roomRepository.findByStayId(stay.getId()).size();
            return new AdminResponse.CompanyStayListDTO(stay,roomsSize);
        }).collect(Collectors.toList());
    }

    // 키워드 search
    public List<AdminResponse.CompanyQuestionListDTO> searchQuestionKeyword(SessionAdmin sessionUser, String keyword){
        if (sessionUser == null){
            new Exception400("로그인이 필요한 서비스입니다");
        }

        List<Question> questionList;

        if (keyword.isBlank()){
            questionList = questionRepository.companyQuestionList();
        } else {
            questionList = questionRepository.findAllKeyword(keyword);
        }

        return questionList.stream().map(question -> {
            Company company = companyRepository.findById(question.getCompany().getId())
            .orElseThrow(() -> new Exception404("해당 기업을 찾을 수 업습니다."));
            return new AdminResponse.CompanyQuestionListDTO(company,question);
        }).toList();

    }

    // 키워드 faq search
    public List<AdminResponse.adminFaqListDTO> searchFaqKeyword(SessionAdmin sessionUser, String keyword){
        if (sessionUser == null){
            new Exception400("로그인이 필요한 서비스입니다");
        }
        List<Faq> faqList;

        if (keyword.isBlank()){
            faqList =  faqRepository.findAllList();
        }else{
            faqList = faqRepository.findAllKeyword(keyword);
        }
        return faqList.stream().map(AdminResponse.adminFaqListDTO::new).toList();
    }


    //[기업 문의사항 리스트]
    public List<AdminResponse.CompanyQuestionListDTO> adminCompanyQuestionList(SessionAdmin sessionUser){

        if (sessionUser == null){
            new Exception400("로그인이 필요한 서비스입니다");
        }

        List<Question> questionList = questionRepository.companyQuestionList();

        List<AdminResponse.CompanyQuestionListDTO> resultList = questionList.stream().map(question -> {

            Company company = companyRepository.findById(question.getCompany().getId())
                    .orElseThrow(() -> new Exception404("해당 기업을 찾을 수 업습니다."));
            return new AdminResponse.CompanyQuestionListDTO(company,question);
        }).toList();

        if (resultList == null){
            resultList = Collections.emptyList();
        }

        return resultList;
    }

    //[기업 문의사항 디테일]
    @Transactional
    public AdminResponse.CompanyQuestionDetailDTO adminCompanyQuestionDetail(Integer questionId, SessionAdmin sessionUser){
        // 인증처리
        if (sessionUser == null){
            new Exception400("로그인이 필요한 서비스입니다");
        }

        Question question = questionRepository.companyQuestionDetail(questionId)
                .orElseThrow(() -> new Exception404("해당 문의사항을 찾을 수 없습니다."));

        Company company = companyRepository.findById(question.getCompany().getId())
                .orElseThrow(() -> new Exception404("해당 기업을 찾을 수 없습니다"));

        return new AdminResponse.CompanyQuestionDetailDTO(company,question);
    }

    //[기업 문의사항 답글작성]
    @Transactional
    public void adminQuestionAnswer(SessionAdmin sessionUser, AdminRequest.AdminAnswerDTO reqDTO){

        // 인증처리
        if (sessionUser == null){
            new Exception400("로그인이 필요한 서비스입니다");
        }

        Question question = questionRepository.findById(reqDTO.getId())
                .orElseThrow(() -> new Exception404("해당 문의를 찾을 수 없습니다"));

        question.updateAnswer(reqDTO);

        questionRepository.save(question);
    }

    //[FAQ 리스트]
    @Transactional
    public List<AdminResponse.adminFaqListDTO> adminFaqList(SessionAdmin sessionUser){

        // 인증처리
        if (sessionUser == null){
            new Exception400("로그인이 필요한 서비스입니다");
        }

        List<Faq> faqList = faqRepository.findAll();

        // 가공해서 담기
        List<AdminResponse.adminFaqListDTO> resultList =
                faqList.stream().map(AdminResponse.adminFaqListDTO::new).toList();

        return resultList;
    }


    //[FAQ 디테일]
    @Transactional
    public AdminResponse.adminFaqDetail adminFaqDetail(Integer faqId, SessionAdmin sessionUser){

        // 인증처리
        if (sessionUser == null){
            new Exception400("로그인이 필요한 서비스입니다");
        }

        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new Exception404("해당 FaQ를 찾을 수 없습니다"));

        return new AdminResponse.adminFaqDetail(faq);
    }

    //[FAQ 작성]
    @Transactional
    public void faqRegister(SessionAdmin sessionUser,AdminRequest.AdminFaqDTO reqDTO){

        // 1.인증처리
        if (sessionUser == null){
            new Exception400("로그인이 필요한 서비스입니다");
        }

        // 2. 권한처리
        Admin admin = adminRepository.findById(reqDTO.getUserId())
                .orElseThrow(() -> new Exception404("해당 관리자를 찾지 못했습니다."));

        // 3. 작성
        faqRepository.save(reqDTO.toEntity(admin));

    }




}
