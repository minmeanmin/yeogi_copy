package com.example.final_project.stay;

import com.example.final_project._core.enums.EventEnum;
import com.example.final_project._core.enums.RoomEnum;
import com.example.final_project._core.errors.exception.Exception400;
import com.example.final_project._core.errors.exception.Exception401;
import com.example.final_project._core.errors.exception.Exception403;
import com.example.final_project._core.errors.exception.Exception404;
import com.example.final_project._core.utils.ImageUtil;
import com.example.final_project.company.Company;
import com.example.final_project.company.CompanyRepository;
import com.example.final_project.company.SessionCompany;
import com.example.final_project.event.Event;
import com.example.final_project.event.EventRepository;
import com.example.final_project.option.Option;
import com.example.final_project.option.OptionRepository;
import com.example.final_project.review.Review;
import com.example.final_project.review.ReviewRepository;
import com.example.final_project.room.Room;
import com.example.final_project.room.RoomRepository;
import com.example.final_project.room_information.RoomInformation;
import com.example.final_project.room_information.RoomInformationRepository;
import com.example.final_project.scrap.Scrap;
import com.example.final_project.scrap.ScrapRepository;
import com.example.final_project.stay_image.StayImage;
import com.example.final_project.stay_image.StayImageRepository;
import com.example.final_project.user.SessionUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.final_project._core.utils.ImageUtil.uploadFiles;

@RequiredArgsConstructor
@Service
public class StayService {
    private final StayRepository stayRepository;
    private final CompanyRepository companyRepository;
    private final OptionRepository optionRepository;
    private final StayImageRepository stayImageRepository;
    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;
    private final RoomInformationRepository roomInformationRepository;
    private final EventRepository eventRepository;
    private final ScrapRepository scrapRepository;

    @Transactional
    public void register(StayRequest.SaveDTO reqDTO, SessionCompany sessionUser) {

        // 인증 처리
        Optional<Company> companyOP = companyRepository.findById(sessionUser.getId());
        Company company = companyOP.orElseThrow(() -> new Exception404("해당 기업을 찾을 수 없습니다"));


        // 권한 처리
        if (!company.getId().equals(sessionUser.getId())) {
            throw new Exception401("숙소를 등록할 권한이 없습니다.");
        }

        // 1.숙소등록
        Stay stay = stayRepository.save(reqDTO.toEntity(company));

        // 2. 이미지 등록
        List<MultipartFile> imgFiles = reqDTO.getImgFiles();
        List<StayImage> stayImages = new ArrayList<>();

        if (imgFiles != null && !imgFiles.isEmpty()) {
            List<ImageUtil.FileUploadResult> uploadResults = uploadFiles(imgFiles);

            for (ImageUtil.FileUploadResult uploadResult : uploadResults) {
                StayImage stayImage = new StayImage();
                stayImage.setName(uploadResult.getFileName());
                stayImage.setPath(uploadResult.getFilePath());
                stayImage.setStay(stay);

                stayImages.add(stayImage);
            }
        }
        stayImageRepository.saveAll(stayImages);
        System.out.println("결과===========" + stayImages);
        // 3.옵션 등록
        if (reqDTO.getOptions() != null && !reqDTO.getOptions().isEmpty()) {
            List<Option> options = reqDTO.getOptions().stream()
                    .map(optionName -> new Option(stay, optionName))
                    .collect(Collectors.toList());

            optionRepository.saveAll(options);
        }
    }

    //숙소 수정폼
    public StayResponse.UpdateFormDTO updateForm(Integer stayId, SessionCompany sessionUser) {
        // 1. 인증 처리
        if (sessionUser == null) {
            throw new Exception400("로그인이 필요한 서비스입니다");
        }

        Stay stay = stayRepository.findByStayId(stayId)
                .orElseThrow(() -> new Exception404("해당 숙소를 찾을 수 없습니다."));

        Company company = companyRepository.findByStayId(stayId)
                .orElseThrow(() -> new Exception404("해당 기업을 찾을 수 없습니다"));

        // 2. 권한 처리
        if (!sessionUser.getId().equals(company.getId())) {
            throw new Exception401("정보를 수정할 권한이 없습니다");
        }

        List<Option> options = optionRepository.findByStayId(stay.getId());

        // Option을 OptionChekedDTO로 변환
        List<StayResponse.UpdateFormDTO.OptionChekedDTO> optionDTOs = new ArrayList<>();
        optionDTOs.add(new StayResponse.UpdateFormDTO.OptionChekedDTO(options));
        return new StayResponse.UpdateFormDTO(stay, optionDTOs);

    }

    //숙소 수정
    @Transactional
    public void update(Integer stayId, SessionCompany sessionCompany, StayRequest.UpdateDTO reqDTO) {

        //1. 인증처리
        Stay stay = stayRepository.findById(stayId)
                .orElseThrow(() -> new Exception404("해당 숙소를 찾을 수 없습니다."));

        //2. 권한처리
        if (stay.getCompany().getId() != sessionCompany.getId()) {
            throw new Exception403("해당 숙소를 수정할 권한이 없습니다.");
        }

        //3. 숙소정보 저장
        stay.updateStay(reqDTO);

        List<Option> beforeOptions = optionRepository.findByStayId(stayId);

        //4. 옵션 삭제
        beforeOptions.clear();
        optionRepository.deleteBystayId(stayId);

        //5. 옵션 저장
        if (reqDTO.getOptions() != null && !reqDTO.getOptions().isEmpty()) {
            List<Option> options = reqDTO.getOptions().stream()
                    .map(optionName -> {
                        return new Option(stay, optionName);
                    })
                    .toList();

            optionRepository.saveAll(options);
        }

    }

    //숙소 삭제
    @Transactional
    public StayResponse.Delete delete(Integer stayId, SessionCompany sessionCompany) {
        //1. 인증처리

        if (sessionCompany.getId() == null) {
            throw new Exception401("로그인이 필요한 서비스입니다.");
        }

        Stay stay = stayRepository.findByStayId(stayId)
                .orElseThrow(() -> new Exception404("해당 숙소를 찾을 수 없습니다."));

        //2. 권한처리
        Company company = companyRepository.findByStayId(stayId)
                .orElseThrow(() -> new Exception404("해당 기업을 찾을 수 업습니다."));

        if (sessionCompany.getId() != company.getId()) {
            throw new Exception403("삭제할 권한이 없습니다");
        }

        //3. 삭제(state 업데이트)
        stay.deleteStay(stay.getState());

        return new StayResponse.Delete(stay);
    }

//    // 숙소 검색 기능 (이름, 지역, 날짜, 가격, 인원 수, 예약 날짜 별 검색) // request 방식
//    public List<StayResponse.SearchListDTO> getSearchStayList(StayRequest.SearchDTO reqDTO) {
//        List<StayResponse.SearchListDTO> resultList;
//
//        resultList = stayRepository.findBySearchStay(reqDTO.getName(), reqDTO.getAddress(), reqDTO.getPrice(), reqDTO.getPerson(), reqDTO.getCheckInDate(), reqDTO.getCheckOutDate()).stream()
//                .map(StayResponse.SearchListDTO::new)
//                .toList();
//
//        return resultList;
//    }

    // 숙소 검색 기능 (이름, 지역, 날짜, 가격, 인원 수 검색)
    public List<StayResponse.SearchListDTO> getSearchStayList(
            String stayName,
            String stayAddress,
            Integer roomPrice,
            Integer person
    ) {
        List<Stay> stayList = stayRepository.findBySearchStay(stayName, stayAddress, roomPrice, person);

        // 숙소 리스트를 매핑하여 결과 리스트 생성
        List<StayResponse.SearchListDTO> resultList = stayList.stream()
                .map(stay -> {
                    // 각 숙소에 대한 이미지 조회
                    StayImage stayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    // stayList 객체 생성
                    return new StayResponse.SearchListDTO(stay, stayImage);
                })
                .collect(Collectors.toList());

        // 결과 리스트가 null이면 빈 리스트로 초기화
        if (resultList == null) {
            resultList = Collections.emptyList();
        }

        // 결과 리스트 반환
        return resultList;
    }

    // 특가숙소
    public List<StayResponse.SaleList> findSpecialListByRoom() {
        RoomEnum state = RoomEnum.APPLIED;

        // 특정 상태에 해당하는 숙소 리스트 조회
        List<Stay> specialList = stayRepository.findStayBySale(state);

        // 조회된 숙소 리스트가 null이면 빈 리스트로 초기화
        if (specialList == null) {
            specialList = Collections.emptyList();
        }

        // 숙소 리스트를 매핑하여 결과 리스트 생성
        List<StayResponse.SaleList> resultList = specialList.stream()
                .map(stay -> {
                    // 각 숙소에 대한 이미지 조회
                    StayImage stayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    // SpecialpriceList 객체 생성
                    return new StayResponse.SaleList(stay, stayImage);
                })
                .collect(Collectors.toList());

        // 결과 리스트가 null이면 빈 리스트로 초기화
        if (resultList == null) {
            resultList = Collections.emptyList();
        }

        // 결과 리스트 반환
        return resultList;
    }

    // 해외숙소
    public List<StayResponse.OverseaList> findOverseaListByCategory() {

        // 해외 숙소찾기
        List<Stay> overSeaList = stayRepository.findStayByOversea();

        // 조회된 숙소 리스트가 null이면 빈 리스트로 초기화
        if (overSeaList == null) {
            overSeaList = Collections.emptyList();
        }

        // 숙소 리스트를 매핑하여 결과 리스트 생성
        List<StayResponse.OverseaList> resultList = overSeaList.stream()
                .map(stay -> {
                    // 각 숙소에 대한 이미지 조회
                    StayImage stayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    // SpecialpriceList 객체 생성
                    return new StayResponse.OverseaList(stay, stayImage);
                })
                .collect(Collectors.toList());

        // 결과 리스트가 null이면 빈 리스트로 초기화
        if (resultList == null) {
            resultList = Collections.emptyList();
        }

        // 결과 리스트 반환
        return resultList;

    }

    // 호텔숙소
    public List<StayResponse.HotelList> findHotelListByCategory() {

        List<Stay> hotelList = stayRepository.findStayByHotel();

        // 조회된 숙소 리스트가 null이면 빈 리스트로 초기화
        if (hotelList == null) {
            hotelList = Collections.emptyList();
        }

        // 숙소 리스트를 매핑하여 결과 리스트 생성
        List<StayResponse.HotelList> resultList = hotelList.stream()
                .map(stay -> {
                    // 각 숙소에 대한 이미지 조회
                    StayImage stayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    // SpecialpriceList 객체 생성
                    return new StayResponse.HotelList(stay, stayImage);
                })
                .collect(Collectors.toList());

        // 결과 리스트가 null이면 빈 리스트로 초기화
        if (resultList == null) {
            resultList = Collections.emptyList();
        }

        // 결과 리스트 반환
        return resultList;
    }

    // 캠핑숙소
    public List<StayResponse.CampingList> findCampingListByCategory() {

        List<Stay> campingList = stayRepository.findStayByCamping();

        // 조회된 숙소 리스트가 null이면 빈 리스트로 초기화
        if (campingList == null) {
            campingList = Collections.emptyList();
        }

        // 숙소 리스트를 매핑하여 결과 리스트 생성
        List<StayResponse.CampingList> resultList = campingList.stream()
                .map(stay -> {
                    // 각 숙소에 대한 이미지 조회
                    StayImage stayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    // SpecialpriceList 객체 생성
                    return new StayResponse.CampingList(stay, stayImage);
                })
                .collect(Collectors.toList());

        // 결과 리스트가 null이면 빈 리스트로 초기화
        if (resultList == null) {
            resultList = Collections.emptyList();
        }

        // 결과 리스트 반환
        return resultList;
    }

    // 모텔숙소
    public List<StayResponse.MotelList> findMotelListByCategory() {

        List<Stay> motelList = stayRepository.findStayByMotel();

        // 조회된 숙소 리스트가 null이면 빈 리스트로 초기화
        if (motelList == null) {
            motelList = Collections.emptyList();
        }

        // 숙소 리스트를 매핑하여 결과 리스트 생성
        List<StayResponse.MotelList> resultList = motelList.stream()
                .map(stay -> {
                    // 각 숙소에 대한 이미지 조회
                    StayImage stayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    // SpecialpriceList 객체 생성
                    return new StayResponse.MotelList(stay, stayImage);
                })
                .collect(Collectors.toList());

        // 결과 리스트가 null이면 빈 리스트로 초기화
        if (resultList == null) {
            resultList = Collections.emptyList();
        }

        // 결과 리스트 반환
        return resultList;
    }

    // 펜션숙소
    public List<StayResponse.PensionList> findPentionByCategory() {

        List<Stay> pentionList = stayRepository.findStayByPention();

        // 조회된 숙소 리스트가 null이면 빈 리스트로 초기화
        if (pentionList == null) {
            pentionList = Collections.emptyList();
        }

        // 숙소 리스트를 매핑하여 결과 리스트 생성
        List<StayResponse.PensionList> resultList = pentionList.stream()
                .map(stay -> {
                    // 각 숙소에 대한 이미지 조회
                    StayImage stayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    // SpecialpriceList 객체 생성
                    return new StayResponse.PensionList(stay, stayImage);
                })
                .collect(Collectors.toList());

        // 결과 리스트가 null이면 빈 리스트로 초기화
        if (resultList == null) {
            resultList = Collections.emptyList();
        }

        // 결과 리스트 반환
        return resultList;
    }

    // 홈&빌라숙소
    public List<StayResponse.HomeAndVillaList> findHomeAndVillaByCategory() {

        List<Stay> homeAndVillaList = stayRepository.findStayByHomeAndVilla();

        // 조회된 숙소 리스트가 null이면 빈 리스트로 초기화
        if (homeAndVillaList == null) {
            homeAndVillaList = Collections.emptyList();
        }

        // 숙소 리스트를 매핑하여 결과 리스트 생성
        List<StayResponse.HomeAndVillaList> resultList = homeAndVillaList.stream()
                .map(stay -> {
                    // 각 숙소에 대한 이미지 조회
                    StayImage stayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    // SpecialpriceList 객체 생성
                    return new StayResponse.HomeAndVillaList(stay, stayImage);
                })
                .collect(Collectors.toList());

        // 결과 리스트가 null이면 빈 리스트로 초기화
        if (resultList == null) {
            resultList = Collections.emptyList();
        }

        // 결과 리스트 반환
        return resultList;
    }

    // 게스트하우스숙소
    public List<StayResponse.GuesthouseList> findGuesthouseByCategory() {

        List<Stay> guesthouseList = stayRepository.findStayByGuesthouse();

        // 조회된 숙소 리스트가 null이면 빈 리스트로 초기화
        if (guesthouseList == null) {
            guesthouseList = Collections.emptyList();
        }

        // 숙소 리스트를 매핑하여 결과 리스트 생성
        List<StayResponse.GuesthouseList> resultList = guesthouseList.stream()
                .map(stay -> {
                    // 각 숙소에 대한 이미지 조회
                    StayImage stayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    // SpecialpriceList 객체 생성
                    return new StayResponse.GuesthouseList(stay, stayImage);
                })
                .collect(Collectors.toList());

        // 결과 리스트가 null이면 빈 리스트로 초기화
        if (resultList == null) {
            resultList = Collections.emptyList();
        }

        // 결과 리스트 반환
        return resultList;
    }


    @Transactional
    public StayResponse.AllList findAllStayWithCategory() {

        // 이벤트
        List<Event> eventList = eventRepository.findAll();
        List<StayResponse.AllList.EventDTO> resultList = eventList.stream()
                .filter(event -> event.getState() == EventEnum.Enable)
                .map(StayResponse.AllList.EventDTO::new).toList();


        // 국내 숙소 찾기
        List<Stay> domesticStays = stayRepository.findAll().stream()
                .filter(stay -> !stay.getCategory().equals("해외"))
                .collect(Collectors.toList());

        System.out.println("국내결과===========================================" + domesticStays.size());

        List<StayResponse.AllList.DomesticDTO> domesticDTOs = domesticStays.stream()
                .map(stay -> {
                    StayImage domesticStayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    String imageName = (domesticStayImage != null) ? domesticStayImage.getName() : null;
                    String imagePath = (domesticStayImage != null) ? domesticStayImage.getPath() : null;
                    return new StayResponse.AllList.DomesticDTO(stay, imageName, imagePath);
                })
                .collect(Collectors.toList());


        // 해외 숙소 찾기
        List<Stay> overseaStays = stayRepository.findAll().stream()
                .filter(stay -> stay.getCategory().equals("해외"))
                .collect(Collectors.toList());

        System.out.println("해외결과===========================================" + overseaStays.size());


        List<StayResponse.AllList.OverseaDTO> overseaDTOs = overseaStays.stream()
                .map(stay -> {
                    StayImage overseaStayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    return new StayResponse.AllList.OverseaDTO(stay, overseaStayImage);
                })
                .collect(Collectors.toList());

        // 특가 숙소 찾기
        List<Stay> specialPriceStays = stayRepository.findAll().stream()
                .filter(stay -> stay.getRooms().stream().anyMatch(room -> room.getSpecialState() == RoomEnum.APPLIED))
                .collect(Collectors.toList());

        System.out.println("특가결과===========================================" + specialPriceStays.size());


        List<StayResponse.AllList.SpecialPriceDTO> specialPriceDTOs = specialPriceStays.stream()
                .map(stay -> {
                    StayImage specialPriceStayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    return new StayResponse.AllList.SpecialPriceDTO(stay, specialPriceStayImage);
                })
                .collect(Collectors.toList());

        return new StayResponse.AllList(specialPriceDTOs, domesticDTOs, overseaDTOs, resultList);
    }

    @Transactional
    public StayResponse.StayDetail findStayDetail(Integer stayId) {
        System.out.println("숙소 번호 : " + stayId);
        // section1 (숙소 이름, 찜 여부, 숙소 이미지, 숙소 리뷰, 숙소 편의시설)
        Stay stay = stayRepository.findByStayId(stayId)
                .orElseThrow(() -> new Exception404("존재하지 않는 숙소입니다.")); // 숙소
        StayResponse.StayDetail.StayContentsDTO.StayDTO stayDTO = new StayResponse.StayDetail.StayContentsDTO.StayDTO(stay);
        //TODO 만약에 ssesionUser로 비교하게되면 추가
//      만약에 boolaen 값 확인하려면 유저가 로그인해서 비교
//      Optional<Scrap> scrapOp = scrapRepository.findByUserIdWithStayId(sessionUser.getId(),stayId);
//      boolean isScrap = scrapOp.isPresent();
//      stayDTO.setScrap(isScrap);

        List<StayImage> stayImageList = stayImageRepository.findByStayId(stayId); // 숙소 이미지
        List<StayResponse.StayDetail.StayContentsDTO.StayImageDTO> stayImageDTOS = stayImageList.stream().map(StayResponse.StayDetail.StayContentsDTO.StayImageDTO::new).toList();


        List<Review> reviewList = reviewRepository.findNoParentReviewByStayIdWithDetails(stayId); // 숙소 리뷰
        List<StayResponse.StayDetail.StayContentsDTO.ReviewDTO> reviewDTOS = reviewList.stream().map(StayResponse.StayDetail.StayContentsDTO.ReviewDTO::new).toList();

        List<Option> optionList = optionRepository.findByStayId(stayId); // 숙소 편의시설
        List<StayResponse.StayDetail.StayContentsDTO.OptionDTO> optionDTOS = optionList.stream().map(StayResponse.StayDetail.StayContentsDTO.OptionDTO::new).collect(Collectors.toList());

        StayResponse.StayDetail.StayContentsDTO stayContentsDTO = new StayResponse.StayDetail.StayContentsDTO(stayDTO, stayImageDTOS, reviewDTOS, optionDTOS);

        // section2 (객실 리스트)
        List<Room> roomList = roomRepository.findByStayId(stayId);
        List<StayResponse.StayDetail.RoomContentsDTO> roomContentsDTOS = roomList.stream().map(room -> {
            RoomInformation roomInformation = roomInformationRepository.findByRoomId(room.getId());
            return new StayResponse.StayDetail.RoomContentsDTO(room, roomInformation);
        }).collect(Collectors.toList());

        // section3 (숙소 소개, 이용 정보, 취소 및 환불 규정) -> 이건 고정값

        return new StayResponse.StayDetail(stayContentsDTO, roomContentsDTOS);
    }


}
